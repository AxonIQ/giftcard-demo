package io.axoniq.demo.giftcard.subscribing;

import org.axonframework.messaging.MetaData;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document
public record SomeSubscribedEntity(
        @Id String id,
        String aggregateIdentifier,
        String payload,
        MetaData metadata
) {

}
