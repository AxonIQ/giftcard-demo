package com.example.giftcard.infra.two_postgres;

import org.axonframework.commandhandling.CommandBus;
import org.axonframework.commandhandling.SimpleCommandBus;
import org.axonframework.common.jdbc.PersistenceExceptionResolver;
import org.axonframework.common.jpa.EntityManagerProvider;
import org.axonframework.common.jpa.SimpleEntityManagerProvider;
import org.axonframework.common.transaction.TransactionManager;
import org.axonframework.eventhandling.EventBus;
import org.axonframework.eventsourcing.eventstore.EmbeddedEventStore;
import org.axonframework.eventsourcing.eventstore.EventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.DomainEventEntry;
import org.axonframework.eventsourcing.eventstore.jpa.JpaEventStorageEngine;
import org.axonframework.eventsourcing.eventstore.jpa.SQLErrorCodesResolver;
import org.axonframework.eventsourcing.eventstore.jpa.SnapshotEventEntry;
import org.axonframework.messaging.interceptors.CorrelationDataInterceptor;
import org.axonframework.monitoring.NoOpMessageMonitor;
import org.axonframework.serialization.Serializer;
import org.axonframework.serialization.upcasting.event.EventUpcaster;
import org.axonframework.spring.config.AxonConfiguration;
import org.axonframework.spring.messaging.unitofwork.SpringTransactionManager;
import org.flywaydb.core.Flyway;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.boot.spi.MetadataImplementor;
import org.hibernate.dialect.PostgreSQL94Dialect;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.hbm2ddl.Target;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.autoconfigure.orm.jpa.JpaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.orm.jpa.EntityManagerFactoryBuilder;
import org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy;
import org.springframework.boot.orm.jpa.hibernate.SpringPhysicalNamingStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Profile("pg_pg")
@Configuration
public class EventStoreJpaConfig {

    /************************************************************************
     * Start with the basic JDBC datasource
     ************************************************************************/

    @Bean
    @Qualifier("events")
    @ConfigurationProperties(prefix = "events.datasource")
    public DataSource eventsDataSource() {
        return DataSourceBuilder.create().build();
    }

    /************************************************************************
     * Using Flyway to do the required schema creation/updates
     ************************************************************************/

    @Bean
    @Qualifier("events")
    @ConfigurationProperties(prefix = "events.flyway")
    public Flyway eventsFlyway(@Qualifier("events") DataSource dataSource) {
        Flyway flyway = new Flyway();
        flyway.setDataSource(dataSource);
        return flyway;
    }

    @Bean
    public FlywayMigrationInitializer eventsFlywayMigrationInitializer(@Qualifier("events") Flyway flyway) {
        return new FlywayMigrationInitializer(flyway);
    }

    /************************************************************************
     * Configuring JPA
     ************************************************************************/

    @Bean
    @Qualifier("events")
    @ConfigurationProperties(prefix = "events.jpa")
    public JpaProperties eventsJpaProperties() {
        return new JpaProperties();
    }

    @Bean
    @Qualifier("events")
    public LocalContainerEntityManagerFactoryBean eventsEntityManagerFactory(
            EntityManagerFactoryBuilder builder,
            @Qualifier("events") DataSource dataSource,
            @Qualifier("events") JpaProperties jpaProperties) {
        return builder
                .dataSource(dataSource)
                .properties(jpaProperties.getProperties())
                .packages("org.axonframework.eventsourcing.eventstore.jpa")
                .persistenceUnit("events")
                .build();
    }

    @Bean
    @Qualifier("events")
    public PlatformTransactionManager eventsPlatformTransactionManager(
            @Qualifier("events") EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @Bean
    @Qualifier("events")
    public EntityManager eventsSharedEntityManager(@Qualifier("events") EntityManagerFactory entityManagerFactory) {
        return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
    }

    /************************************************************************
     * Axon Framework specific things
     ************************************************************************/

    @Bean
    @Qualifier("events")
    public EntityManagerProvider eventsEntityManagerProvider(@Qualifier("events") EntityManager entityManager) {
        return new SimpleEntityManagerProvider(entityManager);
    }

    @Bean
    @Qualifier("events")
    public TransactionManager eventsTransactionManager(@Qualifier("events") PlatformTransactionManager transactionManager) {
        return new SpringTransactionManager(transactionManager);
    }

    @Bean
    @Qualifier("events")
    public PersistenceExceptionResolver eventsDataSourcePER(
            @Qualifier("events") DataSource dataSource) throws SQLException {
        return new SQLErrorCodesResolver(dataSource);
    }

    @Bean
    public EventStorageEngine eventStorageEngine(Serializer serializer,
                                                 @Qualifier("events") PersistenceExceptionResolver persistenceExceptionResolver,
                                                 AxonConfiguration configuration,
                                                 @Qualifier("events") EntityManagerProvider entityManagerProvider,
                                                 @Qualifier("events") TransactionManager transactionManager) {
        return new JpaEventStorageEngine(serializer, configuration.getComponent(EventUpcaster.class),
                persistenceExceptionResolver, configuration.eventSerializer(), null,
                entityManagerProvider, transactionManager, null, null, true);

    }

    @Bean
    public CommandBus commandBus(@Qualifier("events") TransactionManager transactionManager, AxonConfiguration axonConfiguration) {
        SimpleCommandBus commandBus = new SimpleCommandBus(transactionManager, NoOpMessageMonitor.INSTANCE);
        commandBus.registerHandlerInterceptor(new CorrelationDataInterceptor<>(axonConfiguration.correlationDataProviders()));
        return commandBus;
    }

    @Bean
    @Primary
    public EventBus eventBus(EventStorageEngine eventStorageEngine) {
        return new EmbeddedEventStore(eventStorageEngine);
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
        metadataSources.addAnnotatedClass(DomainEventEntry.class);
        metadataSources.addAnnotatedClass(SnapshotEventEntry.class);
        Metadata metadata = metadataSources.buildMetadata();
        SchemaExport schemaExport = new SchemaExport((MetadataImplementor)metadata);
        schemaExport.setFormat(true);
        schemaExport.setDelimiter(";");
        schemaExport.create(Target.SCRIPT);
    }

}
