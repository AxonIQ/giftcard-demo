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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.lang.invoke.MethodHandles;
import java.time.Instant;
import java.util.List;

@Component
public class CardSummaryProjection {

    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final EntityManager entityManager;
    private final TransactionTemplate newTransactionTemplate;
    private final QueryUpdateEmitter queryUpdateEmitter;

    public CardSummaryProjection(EntityManager entityManager, PlatformTransactionManager transactionManager, QueryUpdateEmitter queryUpdateEmitter) {
        this.entityManager = entityManager;
        this.newTransactionTemplate = new TransactionTemplate(transactionManager,
                new DefaultTransactionDefinition(TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        this.queryUpdateEmitter = queryUpdateEmitter;
    }

    @EventHandler
    public void on(IssuedEvt evt, @Timestamp Instant instant) throws InterruptedException {
        log.info("projecting {}", evt);
        newTransactionTemplate.execute(status -> {
            entityManager.persist(new CardSummary(evt.getId(), evt.getAmount(), instant, evt.getAmount()));
            return null;
        });
        queryUpdateEmitter.emit(CountCardSummariesQuery.class, x -> true, new CountChanged());
    }

    @EventHandler
    public void on(RedeemedEvt evt) {
        log.info("projecting {}", evt);
        CardSummary update = newTransactionTemplate.execute(status -> {
            CardSummary summary = entityManager.find(CardSummary.class, evt.getId());
            summary.setRemainingValue(summary.getRemainingValue() - evt.getAmount());
            return summary;
        });
        queryUpdateEmitter.emit(FetchCardSummariesQuery.class, x -> true, update);
    }

    @QueryHandler
    public List<CardSummary> handle(FetchCardSummariesQuery query) {
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
