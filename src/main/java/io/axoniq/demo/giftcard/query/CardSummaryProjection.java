package io.axoniq.demo.giftcard.query;

import io.axoniq.demo.giftcard.api.CardCanceledEvent;
import io.axoniq.demo.giftcard.api.CardIssuedEvent;
import io.axoniq.demo.giftcard.api.CardRedeemedEvent;
import io.axoniq.demo.giftcard.api.CardSummary;
import io.axoniq.demo.giftcard.api.CountCardSummariesQuery;
import io.axoniq.demo.giftcard.api.CountCardSummariesResponse;
import io.axoniq.demo.giftcard.api.FetchCardSummariesQuery;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.Timestamp;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Profile("query")
@Service
@ProcessingGroup("card-summary")
public class CardSummaryProjection {

    private final Map<String, CardSummary> cardSummaryReadModel;
    private final QueryUpdateEmitter queryUpdateEmitter;
    private Instant lastUpdate;

    public CardSummaryProjection(
            QueryUpdateEmitter queryUpdateEmitter
    ) {
        this.cardSummaryReadModel = new ConcurrentHashMap<>();
        this.queryUpdateEmitter = queryUpdateEmitter;
    }

    @EventHandler
    public void on(CardIssuedEvent event, @Timestamp Instant timestamp) {
        lastUpdate = timestamp;
        /*
         * Update our read model by inserting the new card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        CardSummary summary = CardSummary.issue(event.id(), event.amount(), timestamp);
        cardSummaryReadModel.put(event.id(), summary);
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type CountCardSummariesQuery,
         * - for any CountCardSummariesQuery, since true is returned by default, and
         * - send a message that the count of queries matching this query has been changed.
         */
        queryUpdateEmitter.emit(CountCardSummariesQuery.class,
                                query -> true,
                                new CountCardSummariesResponse(cardSummaryReadModel.size(), lastUpdate));
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type CountCardSummariesQuery,
         * - for any CountCardSummariesQuery, since true is returned by default, and
         * - send a message that the count of queries matching this query has been changed.
         */
        queryUpdateEmitter.emit(FetchCardSummariesQuery.class, query -> true, summary);
    }

    @EventHandler
    public void on(CardRedeemedEvent event, @Timestamp Instant timestamp) {
        /*
         * Update our read model by updating the existing card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        CardSummary summary = cardSummaryReadModel.computeIfPresent(
                event.id(), (id, card) -> card.redeem(event.amount(), timestamp)
        );
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type FetchCardSummariesQuery
         * - for any FetchCardSummariesQuery, since true is returned by default, and
         * - send a message containing the new state of this gift card summary
         */
        queryUpdateEmitter.emit(FetchCardSummariesQuery.class, query -> true, summary);
    }

    @EventHandler
    public void on(CardCanceledEvent event, @Timestamp Instant timestamp) {
        /*
         * Update our read model by updating the existing card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        CardSummary summary = cardSummaryReadModel.computeIfPresent(
                event.id(), (id, card) -> card.cancel(timestamp)
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
        return cardSummaryReadModel.values()
                                   .stream()
                                   .sorted(Comparator.comparing(CardSummary::lastUpdated))
                                   .limit(query.limit())
                                   .toList();
    }

    @SuppressWarnings("unused")
    @QueryHandler
    public CountCardSummariesResponse handle(CountCardSummariesQuery query) {
        return new CountCardSummariesResponse(cardSummaryReadModel.size(), lastUpdate);
    }
}
