package io.axoniq.demo.giftcard.query;

import io.axoniq.demo.giftcard.api.event.CardIssuedEvent;
import io.axoniq.demo.giftcard.api.event.CardRedeemedEvent;
import io.axoniq.demo.giftcard.api.query.CardSummary;
import io.axoniq.demo.giftcard.api.query.CountCardSummariesQuery;
import io.axoniq.demo.giftcard.api.query.CountCardSummariesResponse;
import io.axoniq.demo.giftcard.api.query.CountChangedUpdate;
import io.axoniq.demo.giftcard.api.query.FetchCardSummariesQuery;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.stream.Collectors;

@Profile("query")
@Service
@ProcessingGroup("card-summary")
public class CardSummaryProjection {

    private final SortedMap<String, CardSummary> cardSummaryReadModel;
    private final QueryUpdateEmitter queryUpdateEmitter;

    public CardSummaryProjection(
            QueryUpdateEmitter queryUpdateEmitter
    ) {
        this.cardSummaryReadModel = new ConcurrentSkipListMap<>();
        this.queryUpdateEmitter = queryUpdateEmitter;
    }

    @EventHandler
    public void on(CardIssuedEvent event) {
        /*
         * Update our read model by inserting the new card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        cardSummaryReadModel.put(event.id(), new CardSummary(event.id(), event.amount(), event.amount()));
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type CountCardSummariesQuery,
         * - for any CountCardSummariesQuery, since true is returned by default, and
         * - send a message that the count of queries matching this query has been changed.
         */
        queryUpdateEmitter.emit(CountCardSummariesQuery.class, query -> true, new CountChangedUpdate());
    }

    @EventHandler
    public void on(CardRedeemedEvent event) {
        /*
         * Update our read model by updating the existing card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        CardSummary summary = cardSummaryReadModel.computeIfPresent(
                event.id(), (id, card) -> card.redeem(event.amount())
        );
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type FetchCardSummariesQuery
         * - for any FetchCardSummariesQuery, since true is returned by default, and
         * - send a message containing the new state of this gift card summary
         */
        queryUpdateEmitter.emit(FetchCardSummariesQuery.class, query -> true, summary);
    }

    @QueryHandler
    public List<CardSummary> handle(FetchCardSummariesQuery query) {
        CardSummary[] cardSummaryArray = cardSummaryReadModel.values()
                                                             .toArray(CardSummary[]::new);
        return Arrays.stream(cardSummaryArray, query.offset(), cardSummaryArray.length)
                     .limit(query.limit())
                     .collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    @QueryHandler
    public CountCardSummariesResponse handle(CountCardSummariesQuery query) {
        return new CountCardSummariesResponse(cardSummaryReadModel.size(), Instant.now().toEpochMilli());
    }
}
