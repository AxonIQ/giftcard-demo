package com.example.giftcard.gui;

import com.example.giftcard.query.*;
import com.vaadin.data.provider.CallbackDataProvider;
import org.axonframework.eventhandling.*;
import org.axonframework.queryhandling.QueryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.UUID;

public class CardSummaryDataProvider extends CallbackDataProvider<CardSummary, Void> {

    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SubscribingEventProcessor processor;

    public CardSummaryDataProvider(QueryGateway queryGateway, EventBus queryUpdateEventBus) {
        super(
                q -> {
                    FindCardSummariesQuery query = new FindCardSummariesQuery(q.getOffset(), q.getLimit());
                    FindCardSummariesResponse response = queryGateway.send(
                            query, FindCardSummariesResponse.class).join();
                    return response.getData().stream();
                },
                q -> {
                    CountCardSummariesQuery query = new CountCardSummariesQuery();
                    CountCardSummariesResponse response = queryGateway.send(
                            query, CountCardSummariesResponse.class).join();
                    return response.getCount();
                }
        );

        EventListener listener = new AnnotationEventListenerAdapter(this);
        processor = new SubscribingEventProcessor(UUID.randomUUID().toString(),
                new SimpleEventHandlerInvoker(listener), queryUpdateEventBus);
        processor.start();
    }

    public void shutDown() {
        processor.shutDown();
    }

    @EventHandler
    public void on(CardSummariesUpdatedEvt evt) {
        log.info("received {}", evt);
        refreshAll();
    }

}
