package io.axoniq.demo.giftcard.query;

import io.axoniq.demo.giftcard.api.CardIssuedEvent;
import io.axoniq.demo.giftcard.api.CardRedeemedEvent;
import io.axoniq.demo.giftcard.api.CardSummary;
import io.axoniq.demo.giftcard.api.CountCardSummariesQuery;
import io.axoniq.demo.giftcard.api.CountCardSummariesResponse;
import io.axoniq.demo.giftcard.api.FetchCardSummariesQuery;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.junit.jupiter.api.*;

import java.time.Instant;
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

        testSubject.on(new CardIssuedEvent(testId, testAmount), Instant.now());

        List<CardSummary> results = testSubject.handle(new FetchCardSummariesQuery(100));
        assertEquals(1, results.size());
        CardSummary result = results.get(0);
        assertEquals(testId, result.id());
        assertEquals(testAmount, result.initialValue());
        assertEquals(testAmount, result.remainingValue());

        verify(updateEmitter).emit(eq(CountCardSummariesQuery.class), any(), isA(CountCardSummariesResponse.class));
    }

    @Test
    void testCardRedeemedEventUpdatesCardSummaryAndUpdatesFetchCardSummariesQuery() {
        String testId = "001";
        int testAmount = 1377;
        int testRedeemAmount = 42;
        testSubject.on(new CardIssuedEvent(testId, testAmount), Instant.now());

        testSubject.on(new CardRedeemedEvent(testId, testRedeemAmount), Instant.now());

        List<CardSummary> results = testSubject.handle(new FetchCardSummariesQuery(100));
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
        testSubject.on(new CardIssuedEvent(testId, testAmount), Instant.now());

        String otherTestId = "002";
        int otherTestAmount = 42;
        testSubject.on(new CardIssuedEvent(otherTestId, otherTestAmount), Instant.now());

        List<CardSummary> results = testSubject.handle(new FetchCardSummariesQuery(100));
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
        testSubject.on(new CardIssuedEvent(testId, testAmount), Instant.now());
        // second entry
        testSubject.on(new CardIssuedEvent("002", 42), Instant.now());

        List<CardSummary> results = testSubject.handle(new FetchCardSummariesQuery(1));
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
                 .forEach(i -> testSubject.on(new CardIssuedEvent(UUID.randomUUID().toString(), i), Instant.now()));

        CountCardSummariesResponse result = testSubject.handle(new CountCardSummariesQuery());
        assertEquals(expectedCount, result.count());
    }
}