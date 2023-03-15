package io.axoniq.demo.giftcard.configuration;

import org.axonframework.config.ConfigurerModule;
import org.axonframework.eventhandling.PropagatingErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventProcessorConfiguration {

    @Bean
    public ConfigurerModule eventProcessorConfigurerModule() {
        return configurer -> configurer
                .eventProcessing()
                .registerDefaultListenerInvocationErrorHandler(configuration -> PropagatingErrorHandler.instance());
    }
}
