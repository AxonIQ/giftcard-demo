package io.axoniq.demo.giftcard.gui;

import io.axoniq.demo.giftcard.api.CountCardSummariesQuery;
import io.axoniq.demo.giftcard.api.CountCardSummariesResponse;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Profile("gui")
public class GcGuiConfiguration {

    @EventListener(ApplicationReadyEvent.class)
    public void helloHub(ApplicationReadyEvent event) {
        QueryGateway queryGateway = event.getApplicationContext().getBean(QueryGateway.class);
        queryGateway.query(new CountCardSummariesQuery(),
                           ResponseTypes.instanceOf(CountCardSummariesResponse.class));
    }
}
