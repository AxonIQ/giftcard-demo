package com.example.giftcard.infra.two_postgres;

import com.example.giftcard.query.CardSummaryProjection;
import org.axonframework.config.EventHandlingConfiguration;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventhandling.SimpleEventBus;
import org.axonframework.eventhandling.saga.repository.SagaStore;
import org.axonframework.eventhandling.saga.repository.inmemory.InMemorySagaStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("pg_pg")
@Configuration
public class AxonConfig {

    /* Using tracking processors for our read model, which will store there tokens. */
    @Autowired
    public void configure(EventHandlingConfiguration configuration) {
        configuration.registerTrackingProcessor(CardSummaryProjection.class.getPackage().getName());
    }

    /* A non-persistent event bus to push messages from our read model. */
    @Bean
    @Qualifier("queryUpdates")
    public EventBus queryUpdateEventBus() {
        return new SimpleEventBus();
    }

    /* We won't use Sagas. Configuring an in-mem sagastore to avoid auto-creation of a JPA-based instance. */
    @Bean
    public SagaStore sagaStore() {
        return new InMemorySagaStore();
    }
}
