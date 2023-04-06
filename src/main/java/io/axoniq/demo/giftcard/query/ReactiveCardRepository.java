package io.axoniq.demo.giftcard.query;

import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

public interface ReactiveCardRepository extends ReactiveMongoRepository<ReactiveCardEntity, String> {

}