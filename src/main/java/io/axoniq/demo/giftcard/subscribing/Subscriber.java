package io.axoniq.demo.giftcard.subscribing;

import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.DomainEventMessage;
import org.axonframework.eventhandling.EventHandler;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("subscriber")
@Service
@ProcessingGroup("subscriber")
public class Subscriber {

    private final SomeSomeSubscribedRepository someSomeSubscribedRepository;

    public Subscriber(SomeSomeSubscribedRepository someSomeSubscribedRepository) {
        this.someSomeSubscribedRepository = someSomeSubscribedRepository;
    }

    @EventHandler
    public void on(DomainEventMessage<?> eventMessage) {
        var entity = new SomeSubscribedEntity(eventMessage.getIdentifier(),
                                              eventMessage.getAggregateIdentifier(),
                                              eventMessage.getPayload().toString(),
                                              eventMessage.getMetaData());
        someSomeSubscribedRepository.save(entity);
        if(eventMessage.getAggregateIdentifier().endsWith("foo")){
            throw new RuntimeException("Can't end aggregate with foo");
        }
    }
}
