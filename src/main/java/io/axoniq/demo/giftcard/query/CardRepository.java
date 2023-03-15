package io.axoniq.demo.giftcard.query;

import org.springframework.data.mongodb.repository.MongoRepository;

public interface CardRepository extends MongoRepository<CardEntity, String>  {

}