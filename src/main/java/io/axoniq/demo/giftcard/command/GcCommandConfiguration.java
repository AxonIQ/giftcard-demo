package io.axoniq.demo.giftcard.command;

import org.axonframework.common.caching.Cache;
import org.axonframework.common.caching.WeakReferenceCache;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.modelling.command.Repository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("command")
public class GcCommandConfiguration {

    @Bean
    public Repository<GiftCard> giftCardRepository(EventStore eventStore, Cache cache) {
        return EventSourcingRepository.builder(GiftCard.class)
                                      .cache(cache)
                                      .eventStore(eventStore)
                                      .build();
    }

    @Bean
    public Cache cache() {
        return new WeakReferenceCache();
    }
}
