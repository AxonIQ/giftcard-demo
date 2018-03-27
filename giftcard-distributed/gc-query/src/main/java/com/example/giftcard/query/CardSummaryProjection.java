package com.example.giftcard.query;

import com.example.giftcard.api.CanceledEvt;
import com.example.giftcard.api.CardSummary;
import com.example.giftcard.api.CountCardSummariesQuery;
import com.example.giftcard.api.CountCardSummariesResponse;
import com.example.giftcard.api.FindCardSummariesQuery;
import com.example.giftcard.api.FindCardSummariesResponse;
import com.example.giftcard.api.IssuedEvt;
import com.example.giftcard.api.RedeemedEvt;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.Timestamp;
import org.axonframework.queryhandling.QueryHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.time.Instant;
import javax.persistence.EntityManager;
import javax.persistence.Query;

@Component
public class CardSummaryProjection {

    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final EntityManager entityManager;

    public CardSummaryProjection(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @EventHandler
    public void on(IssuedEvt evt, @Timestamp Instant instant) {
        log.debug("projecting {}", evt);
        entityManager.persist(new CardSummary(evt.getId(), evt.getAmount(), instant, evt.getAmount()));
    }

    @EventHandler
    public void on(RedeemedEvt evt) {
        log.debug("projecting {}", evt);
        CardSummary summary = entityManager.find(CardSummary.class, evt.getId());
        summary.setRemainingValue(summary.getRemainingValue() - evt.getAmount());
    }

    @EventHandler
    public void on(CanceledEvt evt) {
        log.debug("projecting {}", evt);
        CardSummary cardSummary = entityManager.getReference(CardSummary.class, evt.getId());
        entityManager.remove(cardSummary);
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
                ((Long) jpaQuery.getSingleResult()).intValue());
        log.debug("returning {}", response);
        return response;
    }
}
