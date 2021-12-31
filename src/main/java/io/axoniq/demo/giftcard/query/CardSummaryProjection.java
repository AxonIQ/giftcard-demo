package io.axoniq.demo.giftcard.query;

import io.axoniq.demo.giftcard.api.CardIssuedEvent;
import io.axoniq.demo.giftcard.api.CardRedeemedEvent;
import io.axoniq.demo.giftcard.api.CardSummary;
import io.axoniq.demo.giftcard.api.CountCardSummariesQuery;
import io.axoniq.demo.giftcard.api.CountCardSummariesResponse;
import io.axoniq.demo.giftcard.api.CountChangedUpdate;
import io.axoniq.demo.giftcard.api.FetchCardSummariesQuery;
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
            @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") QueryUpdateEmitter queryUpdateEmitter
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
        cardSummaryReadModel.put(event.getId(), new CardSummary(event.getId(), event.getAmount(), event.getAmount()));
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
        CardSummary summary = cardSummaryReadModel.get(event.getId());
        summary.setRemainingValue(summary.getRemainingValue() - event.getAmount());
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
        return Arrays.stream(cardSummaryArray, query.getOffset(), cardSummaryArray.length)
                     .limit(query.getLimit())
                     .collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    @QueryHandler
    public CountCardSummariesResponse handle(CountCardSummariesQuery query) {
        return new CountCardSummariesResponse(cardSummaryReadModel.size(), Instant.now().toEpochMilli());
    }
}
