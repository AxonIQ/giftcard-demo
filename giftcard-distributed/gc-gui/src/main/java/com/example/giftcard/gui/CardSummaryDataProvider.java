package com.example.giftcard.gui;

import com.example.giftcard.api.*;
import com.vaadin.data.provider.CallbackDataProvider;
import org.axonframework.queryhandling.QueryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;

public class CardSummaryDataProvider extends CallbackDataProvider<CardSummary, Void> {

    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public CardSummaryDataProvider(QueryGateway queryGateway) {
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
    }

}
