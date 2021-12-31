package io.axoniq.demo.giftcard.query;

import io.axoniq.demo.giftcard.api.CardSummary;
import io.axoniq.demo.giftcard.api.CountCardSummariesQuery;
import io.axoniq.demo.giftcard.api.CountCardSummariesResponse;
import io.axoniq.demo.giftcard.api.CountChangedUpdate;
import io.axoniq.demo.giftcard.api.FetchCardSummariesQuery;
import io.axoniq.demo.giftcard.api.IssuedEvent;
import io.axoniq.demo.giftcard.api.RedeemedEvent;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Profile("query")
@Service
@ProcessingGroup("card-summary")
public class CardSummaryProjection {

    private final Map<String, CardSummary> cardSummaryReadModel;
    private final QueryUpdateEmitter queryUpdateEmitter;

    public CardSummaryProjection(QueryUpdateEmitter queryUpdateEmitter) {
        this.cardSummaryReadModel = new HashMap<>();
        this.queryUpdateEmitter = queryUpdateEmitter;
    }

    @EventHandler
    public void on(IssuedEvent event) {
        /*
         * Update our read model by inserting the new card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        cardSummaryReadModel.put(event.getId(), new CardSummary(event.getId(), event.getAmount(), event.getAmount()));
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type CountCardSummariesQuery
         * - for which is true that the id of the gift card having been issued starts with the idStartWith string
         *   in the query's filter
         * - send a message that the count of queries matching this query has been changed.
         */
        queryUpdateEmitter.emit(CountCardSummariesQuery.class,
                                query -> event.getId().startsWith(query.getFilter().getIdStartsWith()),
                                new CountChangedUpdate());
    }

    @EventHandler
    public void on(RedeemedEvent event) {
        /*
         * Update our read model by updating the existing card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        CardSummary summary = cardSummaryReadModel.get(event.getId());
        summary.setRemainingValue(summary.getRemainingValue() - event.getAmount());
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type FetchCardSummariesQuery
         * - for which is true that the id of the gift card having been redeemed starts with the idStartWith string
         *   in the query's filter
         * - send a message containing the new state of this gift card summary
         */
        queryUpdateEmitter.emit(FetchCardSummariesQuery.class,
                                query -> event.getId().startsWith(query.getFilter().getIdStartsWith()),
                                summary);
    }

    @SuppressWarnings("unused")
    @QueryHandler
    public List<CardSummary> handle(FetchCardSummariesQuery query) {
        return new ArrayList<>(cardSummaryReadModel.values());
    }

    @SuppressWarnings("unused")
    @QueryHandler
    public CountCardSummariesResponse handle(CountCardSummariesQuery query) {
        return new CountCardSummariesResponse(cardSummaryReadModel.size(), Instant.now().toEpochMilli());
    }
}
