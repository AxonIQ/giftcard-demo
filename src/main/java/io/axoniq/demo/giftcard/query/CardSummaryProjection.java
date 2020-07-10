package io.axoniq.demo.giftcard.query;

import io.axoniq.demo.giftcard.api.CardSummary;
import io.axoniq.demo.giftcard.api.CountCardSummariesQuery;
import io.axoniq.demo.giftcard.api.CountCardSummariesResponse;
import io.axoniq.demo.giftcard.api.CountChangedUpdate;
import io.axoniq.demo.giftcard.api.FetchCardSummariesQuery;
import io.axoniq.demo.giftcard.api.IssuedEvt;
import io.axoniq.demo.giftcard.api.RedeemedEvt;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

@Component
@Profile("query")
public class CardSummaryProjection {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final EntityManager entityManager;
    private final QueryUpdateEmitter queryUpdateEmitter;

    public CardSummaryProjection(EntityManager entityManager, QueryUpdateEmitter queryUpdateEmitter) {
        this.entityManager = entityManager;
        this.queryUpdateEmitter = queryUpdateEmitter;
    }

    @EventHandler
    public void on(IssuedEvt event) {
        logger.trace("projecting {}", event);
        /*
         * Update our read model by inserting the new card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        entityManager.persist(new CardSummary(event.getId(), event.getAmount(), event.getAmount()));
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
        logger.trace("projecting {}", event);
        /*
         * Update our read model by updating the existing card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        CardSummary summary = entityManager.find(CardSummary.class, event.getId());
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
        logger.trace("handling {}", query);
        TypedQuery<CardSummary> jpaQuery = entityManager.createNamedQuery("CardSummary.fetch", CardSummary.class);
        jpaQuery.setParameter("idStartsWith", query.getFilter().getIdStartsWith());
        jpaQuery.setFirstResult(query.getOffset());
        jpaQuery.setMaxResults(query.getLimit());
        return jpaQuery.getResultList();
    }

    @QueryHandler
    public CountCardSummariesResponse handle(CountCardSummariesQuery query) {
        logger.trace("handling {}", query);
        TypedQuery<Long> jpaQuery = entityManager.createNamedQuery("CardSummary.count", Long.class);
        jpaQuery.setParameter("idStartsWith", query.getFilter().getIdStartsWith());
        return new CountCardSummariesResponse(jpaQuery.getSingleResult().intValue(), Instant.now().toEpochMilli());
    }
}
