package com.example.giftcard.gui;

import com.example.giftcard.query.CardSummary;
import com.example.giftcard.query.CountCardSummariesQuery;
import com.example.giftcard.query.CountChanged;
import com.example.giftcard.query.FetchCardSummariesQuery;
import com.vaadin.data.provider.AbstractBackEndDataProvider;
import com.vaadin.data.provider.DataChangeEvent;
import com.vaadin.data.provider.DataChangeEvent.DataRefreshEvent;
import com.vaadin.data.provider.Query;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.axonframework.queryhandling.responsetypes.ResponseTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

public class CardSummaryDataProvider extends AbstractBackEndDataProvider<CardSummary, Void> {

    private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    private static final ExecutorService pool = Executors.newSingleThreadExecutor();
    private static final Object CHANGE_QUERY_LOCK = new Object();

    private final QueryGateway queryGateway;
    private SubscriptionQueryResult<List<CardSummary>, CardSummary> fetchQuery;
    private SubscriptionQueryResult<Integer, CountChanged> countQuery;

    public CardSummaryDataProvider(QueryGateway queryGateway) {
        this.queryGateway = queryGateway;
    }

    @Override
    protected Stream<CardSummary> fetchFromBackEnd(Query<CardSummary, Void> query) {
        synchronized (CHANGE_QUERY_LOCK) {
            if (fetchQuery != null) {
                fetchQuery.cancel();
                fetchQuery = null;
            }
            FetchCardSummariesQuery fetchCardSummariesQuery =
                    new FetchCardSummariesQuery(query.getOffset(), query.getLimit());
            log.debug("submitting {}", fetchCardSummariesQuery);
            fetchQuery = queryGateway.subscriptionQuery(fetchCardSummariesQuery,
                    ResponseTypes.multipleInstancesOf(CardSummary.class),
                    ResponseTypes.instanceOf(CardSummary.class));
            fetchQuery.updates().subscribe(
                    cardSummary -> {
                        log.debug("processing update for {}: {}", fetchCardSummariesQuery, cardSummary);
                        fireEvent(new DataRefreshEvent(this, cardSummary));
                    });
            return fetchQuery.initialResult().block().stream();
        }
    }

    @Override
    protected int sizeInBackEnd(Query<CardSummary, Void> query) {
        synchronized (CHANGE_QUERY_LOCK) {
            if (countQuery != null) {
                countQuery.cancel();
                countQuery = null;
            }
            CountCardSummariesQuery countCardSummariesQuery = new CountCardSummariesQuery();
            log.debug("submitting {}", countCardSummariesQuery);
            countQuery = queryGateway.subscriptionQuery(countCardSummariesQuery,
                    ResponseTypes.instanceOf(Integer.class),
                    ResponseTypes.instanceOf(CountChanged.class));
            countQuery.updates().buffer(Duration.ofMillis(250)).subscribe(
                    countChanged -> {
                        log.debug("processing update for {}: {}", countCardSummariesQuery, countChanged);
                        /* This won't do, will lead to immediate new queries, looping a few times. */
//                        fireEvent(new DataChangeEvent(this));
                        pool.execute(() -> {
                            synchronized (CHANGE_QUERY_LOCK) {
                                fireEvent(new DataChangeEvent(this));
                            }
                        });
                    });
            return countQuery.initialResult().block();
        }
    }

    public void shutDown() {
        synchronized (CHANGE_QUERY_LOCK) {
            if (fetchQuery != null) {
                fetchQuery.cancel();
                fetchQuery = null;
            }
            if (countQuery != null) {
                countQuery.cancel();
                countQuery = null;
            }
        }
    }

}
