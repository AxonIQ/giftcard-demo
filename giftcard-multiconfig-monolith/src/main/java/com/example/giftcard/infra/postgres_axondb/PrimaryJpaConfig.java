package com.example.giftcard.infra.postgres_axondb;

import com.example.giftcard.query.CardSummary;
import org.axonframework.eventhandling.tokenstore.jpa.TokenEntry;
import org.flywaydb.core.Flyway;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.PostgreSQL94Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.Target;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

@Profile("pg_axondb")
@Configuration
@EntityScan({"org.axonframework.eventhandling.tokenstore.jpa", "com.example.giftcard.query"})
public class PrimaryJpaConfig  {

    /************************************************************************
     * Start with the basic JDBC datasource
     ************************************************************************/

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "primary.datasource")
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }

    /************************************************************************
     * Using Flyway to do the required schema creation/updates
     ************************************************************************/

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "primary.flyway")
    public Flyway flyway(DataSource dataSource) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        return flyway;
    }

    @Bean
    public FlywayMigrationInitializer flywayMigrationInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }

    /************************************************************************
     * Configuring JPA
     ************************************************************************/

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "primary.jpa")
    public JpaProperties jpaProperties() {
        return new JpaProperties();
    }

    /************************************************************************
     * Simple utility to generate an initial version of the DDL
     ************************************************************************/

    public static void main(String[] args) {
        Map<String, Object> settings = new HashMap<>();
        settings.put("hibernate.dialect", PostgreSQL94Dialect.class);
        settings.put("hibernate.implicit_naming_strategy", SpringImplicitNamingStrategy.class);
        settings.put("hibernate.physical_naming_strategy", SpringPhysicalNamingStrategy.class);
        StandardServiceRegistry standardServiceRegistry = new StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build();
        MetadataSources metadataSources = new MetadataSources(standardServiceRegistry);
        metadataSources.addAnnotatedClass(TokenEntry.class);
        metadataSources.addAnnotatedClass(CardSummary.class);
        Metadata metadata = metadataSources.buildMetadata();
        SchemaExport schemaExport = new SchemaExport((MetadataImplementor)metadata);
        schemaExport.setFormat(true);
        schemaExport.setDelimiter(";");
        schemaExport.create(Target.SCRIPT);
    }

}
