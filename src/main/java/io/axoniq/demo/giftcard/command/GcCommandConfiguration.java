package io.axoniq.demo.giftcard.command;

import org.axonframework.common.caching.Cache;
import org.axonframework.common.caching.WeakReferenceCache;
import org.axonframework.eventsourcing.EventSourcingRepository;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.axonframework.modelling.command.Repository;

import javax.enterprise.inject.Produces;

public class GcCommandConfiguration {

    @Produces
    public Repository<GiftCard> giftCardRepository(EventStore eventStore, Cache cache) {
        return EventSourcingRepository.builder(GiftCard.class)
                                      .cache(cache)
                                      .eventStore(eventStore)
                                      .build();
    }

    @Produces
    public Cache cache() {
        return new WeakReferenceCache();
    }
}
