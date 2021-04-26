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
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

@Profile("query")
@Service
@ProcessingGroup("card-summary")
public class CardSummaryProjection {

    private final EntityManager entityManager;
    private final QueryUpdateEmitter queryUpdateEmitter;

    public CardSummaryProjection(EntityManager entityManager,
                                 @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") QueryUpdateEmitter queryUpdateEmitter) {
        this.entityManager = entityManager;
        this.queryUpdateEmitter = queryUpdateEmitter;
    }

    /*
     * Update our read model by inserting the new card. This is done so that upcoming regular
     * (non-subscription) queries get correct data.
     *
     * Serve the subscribed queries by emitting an update. This reads as follows:
     * - to all current subscriptions of type CountCardSummariesQuery
     * - for which is true that the id of the gift card having been issued starts with the idStartWith string
     *   in the query's filter
     * - send a message that the count of queries matching this query has been changed.
     */
    @EventHandler
    public void on(IssuedEvent event) {
        entityManager.persist(new CardSummary(event.getId(), event.getAmount(), event.getAmount()));

        queryUpdateEmitter.emit(CountCardSummariesQuery.class,
                                query -> event.getId().startsWith(query.getFilter().getIdStartsWith()),
                                new CountChangedUpdate());
    }

    /*
     * Update our read model by updating the existing card. This is done so that upcoming regular
     * (non-subscription) queries get correct data.
     *
     * Serve the subscribed queries by emitting an update. This reads as follows:
     * - to all current subscriptions of type FetchCardSummariesQuery
     * - for which is true that the id of the gift card having been redeemed starts with the idStartWith string
     *   in the query's filter
     * - send a message containing the new state of this gift card summary
     */
    @EventHandler
    public void on(RedeemedEvent event) {
        CardSummary summary = entityManager.find(CardSummary.class, event.getId());
        summary.setRemainingValue(summary.getRemainingValue() - event.getAmount());

        queryUpdateEmitter.emit(FetchCardSummariesQuery.class,
                                query -> event.getId().startsWith(query.getFilter().getIdStartsWith()),
                                summary);
    }

    @SuppressWarnings("unused")
    @QueryHandler
    public List<CardSummary> handle(FetchCardSummariesQuery query) {
        TypedQuery<CardSummary> jpaQuery = entityManager.createNamedQuery("CardSummary.fetch", CardSummary.class);
        jpaQuery.setParameter("idStartsWith", query.getFilter().getIdStartsWith());
        jpaQuery.setFirstResult(query.getOffset());
        jpaQuery.setMaxResults(query.getLimit());
        return jpaQuery.getResultList();
    }

    @SuppressWarnings("unused")
    @QueryHandler
    public CountCardSummariesResponse handle(CountCardSummariesQuery query) {
        TypedQuery<Long> jpaQuery = entityManager.createNamedQuery("CardSummary.count", Long.class);
        jpaQuery.setParameter("idStartsWith", query.getFilter().getIdStartsWith());
        return new CountCardSummariesResponse(jpaQuery.getSingleResult().intValue(), Instant.now().toEpochMilli());
    }
}
