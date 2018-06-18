package com.example.giftcard.gui;

import com.example.giftcard.api.CardSummary;
import com.example.giftcard.api.CountCardSummariesQuery;
import com.example.giftcard.api.CountCardSummariesResponse;
import com.example.giftcard.api.FindCardSummariesQuery;
import com.example.giftcard.api.FindCardSummariesResponse;
import com.vaadin.data.provider.CallbackDataProvider;
import org.axonframework.queryhandling.QueryGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

public class CardSummaryDataProvider extends CallbackDataProvider<CardSummary, Void> {

    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private final Set<Consumer<CountCardSummariesResponse>> countListeners = new HashSet<>();

    public CardSummaryDataProvider(QueryGateway queryGateway) {
        super(
                q -> {
                    FindCardSummariesQuery query = new FindCardSummariesQuery(q.getOffset(), q.getLimit());
                    FindCardSummariesResponse response = queryGateway.query(
                            query, FindCardSummariesResponse.class).join();
                    return response.getData().stream();
                },
                q -> {
                    CountCardSummariesQuery query = new CountCardSummariesQuery();
                    CountCardSummariesResponse response = queryGateway.query(
                            query, CountCardSummariesResponse.class).join();
                    return response.getCount();
                }
        );
    }
}
