package com.example.giftcard.gui;

import com.example.giftcard.query.*;
import com.vaadin.data.provider.CallbackDataProvider;
import org.axonframework.eventhandling.*;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.axonframework.queryhandling.responsetypes.ResponseTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.smartcardio.Card;
import java.lang.invoke.MethodHandles;
import java.util.UUID;

public class CardSummaryDataProvider extends CallbackDataProvider<CardSummary, Void> {

    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final SubscriptionQueryResult<Integer, CardProjectionUpdated> subscriptionQueryResult;

    public CardSummaryDataProvider(QueryGateway queryGateway) {
        super(
                q -> {
                    FindCardSummariesQuery query = new FindCardSummariesQuery(q.getOffset(), q.getLimit());
                    return queryGateway.query(query, ResponseTypes.multipleInstancesOf(CardSummary.class))
                            .join().stream();
                },
                q -> {
                    CountCardSummariesQuery query = new CountCardSummariesQuery();
                    return queryGateway.query(query, ResponseTypes.instanceOf(Integer.class)).join();
                }
        );

        subscriptionQueryResult = queryGateway.subscriptionQuery(new CountCardSummariesQuery(), ResponseTypes.instanceOf(Integer.class),
                ResponseTypes.instanceOf(CardProjectionUpdated.class));

        subscriptionQueryResult.handle(x -> {}, x -> {
            log.debug("received query update, refreshing data provider");
            refreshAll();
        });
    }

    public void shutDown() {
        subscriptionQueryResult.cancel();
    }

}
