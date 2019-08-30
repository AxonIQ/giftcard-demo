package io.axoniq.demo.giftcard;

import org.axonframework.config.EventProcessingConfigurer;
import org.axonframework.config.EventProcessingModule;
import org.axonframework.eventhandling.TrackingEventProcessorConfiguration;
import org.axonframework.queryhandling.DefaultQueryGateway;
import org.axonframework.queryhandling.QueryBus;
import org.axonframework.queryhandling.QueryGateway;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;

@ApplicationScoped
public class GiftCardApp {

//    @Produces
//    public EventProcessingConfigurer config() {
//        EventProcessingConfigurer configurer = new EventProcessingModule();
//
//        configurer.registerTrackingEventProcessorConfiguration(
//                c -> TrackingEventProcessorConfiguration.forParallelProcessing(2)
//        );
//        return configurer;
//    }
//
//    @Produces
//    public QueryGateway queryGateway(QueryBus queryBus) {
//        return DefaultQueryGateway
//                .builder()
//                .queryBus(queryBus)
//                .build();
//    }
}
