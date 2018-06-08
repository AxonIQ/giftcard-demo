package com.example.giftcard.command;

import org.axonframework.commandhandling.model.Repository;
import org.axonframework.common.caching.Cache;
import org.axonframework.common.caching.WeakReferenceCache;
import org.axonframework.eventsourcing.CachingEventSourcingRepository;
import org.axonframework.eventsourcing.GenericAggregateFactory;
import org.axonframework.eventsourcing.eventstore.EventStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GcCommandApp {

	public static void main(String[] args) {
		SpringApplication.run(GcCommandApp.class, args);
	}

	@Bean
	public Repository<GiftCard> giftCardRepository(EventStore eventStore, Cache cache) {
		return new CachingEventSourcingRepository<>(new GenericAggregateFactory<>(GiftCard.class), eventStore, cache);
	}

	@Bean
	public Cache cache() {
		return new WeakReferenceCache();
	}
}
