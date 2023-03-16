package io.axoniq.demo.giftcard.query;

import io.axoniq.demo.giftcard.api.CardCanceledEvent;
import io.axoniq.demo.giftcard.api.CardIssuedEvent;
import io.axoniq.demo.giftcard.api.CardRedeemedEvent;
import io.axoniq.demo.giftcard.api.CardSummary;
import io.axoniq.demo.giftcard.api.CountCardSummariesQuery;
import io.axoniq.demo.giftcard.api.CountCardSummariesResponse;
import io.axoniq.demo.giftcard.api.FetchCardSummariesQuery;
import org.axonframework.config.ProcessingGroup;
import org.axonframework.eventhandling.EventHandler;
import org.axonframework.eventhandling.Timestamp;
import org.axonframework.queryhandling.QueryHandler;
import org.axonframework.queryhandling.QueryUpdateEmitter;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.collect.Lists.reverse;
import static org.springframework.data.domain.Sort.by;

@Profile("query")
@Service
@ProcessingGroup("card-summary")
public class CardSummaryProjection {

    private final CardRepository cardRepository;
    private final QueryUpdateEmitter queryUpdateEmitter;

    public CardSummaryProjection(
            CardRepository cardRepository,
            QueryUpdateEmitter queryUpdateEmitter
    ) {
        this.cardRepository = cardRepository;
        this.queryUpdateEmitter = queryUpdateEmitter;
    }

    @EventHandler
    public void on(CardIssuedEvent event, @Timestamp Instant timestamp) {
        /*
         * Update our read model by inserting the new card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        CardEntity entity = CardEntity.issue(event.id(), event.amount(), timestamp);
        cardRepository.save(entity);
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type CountCardSummariesQuery,
         * - for any CountCardSummariesQuery, since true is returned by default, and
         * - send a message that the count of queries matching this query has been changed.
         */
        queryUpdateEmitter.emit(CountCardSummariesQuery.class,
                                query -> true,
                                new CountCardSummariesResponse((int) cardRepository.count(), timestamp));
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type CountCardSummariesQuery,
         * - for any CountCardSummariesQuery, since true is returned by default, and
         * - send a message that the count of queries matching this query has been changed.
         */
        queryUpdateEmitter.emit(FetchCardSummariesQuery.class, query -> true, entity.toSummary());
    }

    @EventHandler
    public void on(CardRedeemedEvent event, @Timestamp Instant timestamp) {
        /*
         * Update our read model by updating the existing card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        CardEntity entity = cardRepository.findById(event.id()).orElseThrow().redeem(event.amount(), timestamp);
        cardRepository.save(entity);
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type FetchCardSummariesQuery
         * - for any FetchCardSummariesQuery, since true is returned by default, and
         * - send a message containing the new state of this gift card summary
         */
        queryUpdateEmitter.emit(FetchCardSummariesQuery.class, query -> true, entity.toSummary());
    }

    @EventHandler
    public void on(CardCanceledEvent event, @Timestamp Instant timestamp) {
        /*
         * Update our read model by updating the existing card. This is done so that upcoming regular
         * (non-subscription) queries get correct data.
         */
        CardEntity entity = cardRepository.findById(event.id()).orElseThrow().cancel(timestamp);
        cardRepository.save(entity);
        /*
         * Serve the subscribed queries by emitting an update. This reads as follows:
         * - to all current subscriptions of type FetchCardSummariesQuery
         * - for any FetchCardSummariesQuery, since true is returned by default, and
         * - send a message containing the new state of this gift card summary
         */
        queryUpdateEmitter.emit(FetchCardSummariesQuery.class, query -> true, entity.toSummary());
    }

    @QueryHandler
    public List<CardSummary> handle(FetchCardSummariesQuery query) {
        PageRequest pageRequest = PageRequest.of(0, query.limit(), by(Sort.Direction.DESC,"lastUpdated"));
        return reverse(cardRepository.findAll(pageRequest)
                .map(CardEntity::toSummary)
                .toList());
    }

    @SuppressWarnings("unused")
    @QueryHandler
    public CountCardSummariesResponse handle(CountCardSummariesQuery query) {
        AtomicReference<Instant> time = new AtomicReference<>(Instant.MIN);
        PageRequest pageRequest = PageRequest.of(0, 1, by(Sort.Direction.DESC, "lastUpdated"));
        cardRepository.findAll(pageRequest).stream().findFirst().ifPresent(c -> time.set(c.lastUpdated()));
        return new CountCardSummariesResponse((int) cardRepository.count(), time.get());
    }
}
