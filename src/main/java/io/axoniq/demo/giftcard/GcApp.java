package io.axoniq.demo.giftcard;

import org.axonframework.config.EventProcessingConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GcApp {

    public static void main(String[] args) {
        SpringApplication.run(GcApp.class, args);
    }

    @Autowired
    public void configure(EventProcessingConfiguration eventProcessingConfiguration) {
        eventProcessingConfiguration.usingTrackingProcessors();
    }

}
