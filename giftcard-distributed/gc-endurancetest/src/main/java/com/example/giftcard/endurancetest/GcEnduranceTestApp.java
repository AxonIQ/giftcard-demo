package com.example.giftcard.endurancetest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

/**
 * @author Milan Savic
 */
@SpringBootApplication
@EnableSwagger2
public class GcEnduranceTestApp {

    public static void main(String[] args) {
        SpringApplication.run(GcEnduranceTestApp.class);
    }

    @Bean
    public Docket swagger() {
        return new Docket(DocumentationType.SWAGGER_2).select().build();
    }
}
