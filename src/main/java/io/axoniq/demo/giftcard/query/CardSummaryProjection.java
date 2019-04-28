package io.axoniq.demo.giftcard.query;

import io.axoniq.demo.giftcard.api.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.XSlf4j;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@XSlf4j
@RequiredArgsConstructor
@Profile("query")
public class CardSummaryProjection {

    private final Map<String, CardSummary> cardSummaryReadModel = new HashMap();
    private final QueryUpdateEmitter queryUpdateEmitter;

    @EventHandler
    public void on(IssuedEvt event) {
        log.trace("projecting {}", event);
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
    public void on(RedeemedEvt event) {
        log.trace("projecting {}", event);
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

    @QueryHandler
    public List<CardSummary> handle(FetchCardSummariesQuery query) {
        log.trace("handling {}", query);
        return log.exit(new ArrayList(cardSummaryReadModel.values()));
    }

    @QueryHandler
    public CountCardSummariesResponse handle(CountCardSummariesQuery query) {
        log.trace("handling {}", query);
        return log.exit(new CountCardSummariesResponse(cardSummaryReadModel.size(), Instant.now().toEpochMilli()));
    }

}
