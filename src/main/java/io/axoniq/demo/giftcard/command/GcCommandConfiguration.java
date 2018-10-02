package io.axoniq.demo.giftcard.command;

import org.axonframework.commandhandling.model.Repository;
import org.axonframework.common.caching.Cache;
import org.axonframework.common.caching.WeakReferenceCache;
import org.axonframework.eventsourcing.CachingEventSourcingRepository;
import org.axonframework.eventsourcing.GenericAggregateFactory;
import org.axonframework.eventsourcing.eventstore.EventStore;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("command")
public class GcCommandConfiguration {

	@Bean
	public Repository<GiftCard> giftCardRepository(EventStore eventStore, Cache cache) {
		return new CachingEventSourcingRepository<>(new GenericAggregateFactory<>(GiftCard.class), eventStore, cache);
	}

	@Bean
    public Cache cache() {
	    return new WeakReferenceCache();
	}
}
