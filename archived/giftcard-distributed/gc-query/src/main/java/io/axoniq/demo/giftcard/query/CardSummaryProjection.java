package io.axoniq.demo.giftcard.query;

import io.axoniq.demo.giftcard.api.*;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.Timestamp;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class CardSummaryProjection {

    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final EntityManager entityManager;
    private AtomicReference<Instant> lastEvent = new AtomicReference<>(Instant.EPOCH);

    public CardSummaryProjection(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @EventHandler
    public void on(IssuedEvt evt, @Timestamp Instant instant) {
        log.debug("projecting {}", evt);
        updateLastEvent(instant);
        entityManager.persist(new CardSummary(evt.getId(), evt.getAmount(), instant, evt.getAmount()));
    }

    @EventHandler
    public void on(RedeemedEvt evt, @Timestamp Instant instant) {
        log.debug("projecting {}", evt);
        updateLastEvent(instant);
        CardSummary summary = entityManager.find(CardSummary.class, evt.getId());
        if (summary != null) {
            summary.setRemainingValue(summary.getRemainingValue() - evt.getAmount());
        } else {
            log.warn("Card not found: {}", evt.getId());
        }
    }

    @EventHandler
    public void on(CancelEvt evt, @Timestamp Instant instant) {
        updateLastEvent(instant);
        CardSummary summary = entityManager.find(CardSummary.class, evt.getId());
        if( summary != null) {
            entityManager.remove(summary);
        }
    }

    @QueryHandler
    public FindCardSummariesResponse handle(FindCardSummariesQuery query) {
        log.debug("handling {}", query);
        Query jpaQuery = entityManager.createQuery("SELECT c FROM CardSummary c ORDER BY c.id",
                CardSummary.class);
        jpaQuery.setFirstResult(query.getOffset());
        jpaQuery.setMaxResults(query.getLimit());
        FindCardSummariesResponse response = new FindCardSummariesResponse(jpaQuery.getResultList());
        log.debug("returning {}", response);
        return response;
    }

    @QueryHandler
    public CountCardSummariesResponse handle(CountCardSummariesQuery query) {
        log.debug("handling {}", query);
        Query jpaQuery = entityManager.createQuery("SELECT COUNT(c) FROM CardSummary c",
                Long.class);
        CountCardSummariesResponse response = new CountCardSummariesResponse(
                ((Long)jpaQuery.getSingleResult()).intValue(), lastEvent.get().toEpochMilli());
        log.debug("returning {}", response);
        return response;
    }

    private void updateLastEvent(Instant instant) {
        lastEvent.accumulateAndGet(instant, (old,newInstant) -> newInstant != null && newInstant.isAfter(old) ? newInstant : old);
    }
}
