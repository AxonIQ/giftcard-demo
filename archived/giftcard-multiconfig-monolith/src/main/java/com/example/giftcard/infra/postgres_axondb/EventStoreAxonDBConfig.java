package com.example.giftcard.infra.postgres_axondb;

import io.axoniq.axondb.client.AxonDBConfiguration;
import io.axoniq.axondb.client.axon.AxonDBEventStore;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.serialization.Serializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("pg_axondb")
@Configuration
public class EventStoreAxonDBConfig {

    @Bean
    public AxonDBConfiguration axonDBConfiguration() {
        return new AxonDBConfiguration();
    }

    @Bean
    @Primary
    public EventBus eventBus(AxonDBConfiguration axonDBConfiguration, Serializer serializer) {
        return new AxonDBEventStore(axonDBConfiguration, serializer);
    }

}
