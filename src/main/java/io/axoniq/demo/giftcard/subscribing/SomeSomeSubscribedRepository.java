package io.axoniq.demo.giftcard.subscribing;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface SomeSomeSubscribedRepository extends MongoRepository<SomeSubscribedEntity, String> {

}
