package io.axoniq.demo.giftcard.query;

import io.axoniq.demo.giftcard.api.event.CardIssuedEvent;
import io.axoniq.demo.giftcard.api.event.CardRedeemedEvent;
import io.axoniq.demo.giftcard.api.query.CardSummary;
import io.axoniq.demo.giftcard.api.query.CountCardSummariesQuery;
import io.axoniq.demo.giftcard.api.query.CountCardSummariesResponse;
import io.axoniq.demo.giftcard.api.query.CountChangedUpdate;
import io.axoniq.demo.giftcard.api.query.FetchCardSummariesQuery;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.junit.jupiter.api.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CardSummaryProjectionTest {

    private final QueryUpdateEmitter updateEmitter = mock(QueryUpdateEmitter.class);

    private CardSummaryProjection testSubject;

    @BeforeEach
    void setUp() {
        testSubject = new CardSummaryProjection(updateEmitter);
    }

    @Test
    void testCardIssuedEventInsertsCardSummaryAndUpdatesCountCardSummariesQuery() {
        String testId = "001";
        int testAmount = 1377;

        testSubject.on(new CardIssuedEvent(testId, testAmount));

        List<CardSummary> results = testSubject.handle(new FetchCardSummariesQuery(0, 100));
        assertEquals(1, results.size());
        CardSummary result = results.get(0);
        assertEquals(testId, result.id());
        assertEquals(testAmount, result.initialValue());
        assertEquals(testAmount, result.remainingValue());

        verify(updateEmitter).emit(eq(CountCardSummariesQuery.class), any(), isA(CountChangedUpdate.class));
    }

    @Test
    void testCardRedeemedEventUpdatesCardSummaryAndUpdatesFetchCardSummariesQuery() {
        String testId = "001";
        int testAmount = 1377;
        int testRedeemAmount = 42;
        testSubject.on(new CardIssuedEvent(testId, testAmount));

        testSubject.on(new CardRedeemedEvent(testId, testRedeemAmount));

        List<CardSummary> results = testSubject.handle(new FetchCardSummariesQuery(0, 100));
        assertEquals(1, results.size());
        CardSummary result = results.get(0);
        assertEquals(testId, result.id());
        assertEquals(testAmount, result.initialValue());
        assertEquals(testAmount - testRedeemAmount, result.remainingValue());

        verify(updateEmitter).emit(eq(FetchCardSummariesQuery.class), any(), eq(result));
    }

    @Test
    void testFetchCardSummariesQueryReturnsAllCardSummaries() {
        String testId = "001";
        int testAmount = 1377;
        testSubject.on(new CardIssuedEvent(testId, testAmount));

        String otherTestId = "002";
        int otherTestAmount = 42;
        testSubject.on(new CardIssuedEvent(otherTestId, otherTestAmount));

        List<CardSummary> results = testSubject.handle(new FetchCardSummariesQuery(0, 100));
        assertEquals(2, results.size());

        CardSummary firstResult = results.get(0);
        assertEquals(testId, firstResult.id());
        assertEquals(testAmount, firstResult.initialValue());
        assertEquals(testAmount, firstResult.remainingValue());

        CardSummary secondResult = results.get(1);
        assertEquals(otherTestId, secondResult.id());
        assertEquals(otherTestAmount, secondResult.initialValue());
        assertEquals(otherTestAmount, secondResult.remainingValue());
    }

    @Test
    void testFetchCardSummariesQueryReturnsFirstEntryOnLimitedSetOfCardSummaries() {
        String testId = "001";
        int testAmount = 1377;
        // first entry
        testSubject.on(new CardIssuedEvent(testId, testAmount));
        // second entry
        testSubject.on(new CardIssuedEvent("002", 42));

        List<CardSummary> results = testSubject.handle(new FetchCardSummariesQuery(0, 1));
        assertEquals(1, results.size());
        CardSummary result = results.get(0);
        assertEquals(testId, result.id());
        assertEquals(testAmount, result.initialValue());
        assertEquals(testAmount, result.remainingValue());
    }

    @Test
    void testFetchCardSummariesQueryReturnsSecondEntryOnLimitedSetOfCardSummaries() {
        String testId = "002";
        int testAmount = 1377;
        // first entry
        testSubject.on(new CardIssuedEvent("001", 42));
        // second entry
        testSubject.on(new CardIssuedEvent(testId, testAmount));

        List<CardSummary> results = testSubject.handle(new FetchCardSummariesQuery(1, 1));
        assertEquals(1, results.size());
        CardSummary result = results.get(0);
        assertEquals(testId, result.id());
        assertEquals(testAmount, result.initialValue());
        assertEquals(testAmount, result.remainingValue());
    }

    @Test
    void testCountCardSummariesQueryReturnsNumberOfCardSummaryEntries() {
        int expectedCount = 10;
        IntStream.range(0, expectedCount)
                 .forEach(i -> testSubject.on(new CardIssuedEvent(UUID.randomUUID().toString(), i)));

        CountCardSummariesResponse result = testSubject.handle(new CountCardSummariesQuery());
        assertEquals(expectedCount, result.count());
    }
}