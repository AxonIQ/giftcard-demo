package com.example.giftcard.query;

import com.example.giftcard.command.IssuedEvt;
import com.example.giftcard.command.RedeemedEvt;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.Timestamp;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.List;

@Component
public class CardSummaryProjection {

    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final EntityManager entityManager;
    private final QueryUpdateEmitter queryUpdateEmitter;

    public CardSummaryProjection(EntityManager entityManager, QueryUpdateEmitter queryUpdateEmitter) {
        this.entityManager = entityManager;
        this.queryUpdateEmitter = queryUpdateEmitter;
    }

    @EventHandler
    public void on(IssuedEvt evt, @Timestamp Instant instant) {
        log.info("projecting {}", evt);
        entityManager.persist(new CardSummary(evt.getId(), evt.getAmount(), instant, evt.getAmount()));
        queryUpdateEmitter.emit(CountCardSummariesQuery.class, x -> true, new CardProjectionUpdated());
    }

    @EventHandler
    public void on(RedeemedEvt evt) {
        log.info("projecting {}", evt);
        CardSummary summary = entityManager.find(CardSummary.class, evt.getId());
        summary.setRemainingValue(summary.getRemainingValue() - evt.getAmount());
        queryUpdateEmitter.emit(CountCardSummariesQuery.class, x -> true, new CardProjectionUpdated());
    }

    @QueryHandler
    public List<CardSummary> handle(FindCardSummariesQuery query) {
        log.info("handling {}", query);
        Query jpaQuery = entityManager.createQuery("SELECT c FROM CardSummary c ORDER BY c.id",
                CardSummary.class);
        jpaQuery.setFirstResult(query.getOffset());
        jpaQuery.setMaxResults(query.getLimit());
        List<CardSummary> response = jpaQuery.getResultList();
        log.info("returning {}", response);
        return response;
    }

    @QueryHandler
    public Integer handle(CountCardSummariesQuery query) {
        log.info("handling {}", query);
        Query jpaQuery = entityManager.createQuery("SELECT COUNT(c) FROM CardSummary c",
                Long.class);
        Integer response = ((Long)jpaQuery.getSingleResult()).intValue();
        log.info("returning {}", response);
        return response;
    }
}
