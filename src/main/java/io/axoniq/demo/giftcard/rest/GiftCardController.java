package io.axoniq.demo.giftcard.rest;

import io.axoniq.demo.giftcard.api.IssueCardCommand;
import io.axoniq.demo.giftcard.api.RedeemCardCommand;
import io.axoniq.demo.giftcard.api.CardSummary;
import io.axoniq.demo.giftcard.api.CountCardSummariesQuery;
import io.axoniq.demo.giftcard.api.CountCardSummariesResponse;
import io.axoniq.demo.giftcard.api.FetchCardSummariesQuery;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.axonframework.messaging.responsetypes.ResponseTypes;
import org.axonframework.queryhandling.QueryGateway;
import org.axonframework.queryhandling.SubscriptionQueryResult;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@RestController
@Profile("gui")
@RequestMapping("/giftcard")
public class GiftCardController {

    private final CommandGateway commandGateway;
    private final QueryGateway queryGateway;

    public GiftCardController(CommandGateway commandGateway, QueryGateway queryGateway) {
        this.commandGateway = commandGateway;
        this.queryGateway = queryGateway;
    }

    @GetMapping(path = "/subscribe/limit/{limit}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CardSummary> subscribe(
            @PathVariable int limit
    ) {
        return summerySubscription(limit);
    }

    @GetMapping(path = "/subscribe-count", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<CountCardSummariesResponse> subscribe() {
        return countSubscription();
    }

    @PostMapping("issue/id/{id}/amount/{amount}")
    public Mono<Result> issue(
            @PathVariable String id,
            @PathVariable int amount
    ) {
        var command = new IssueCardCommand(id, amount);
        return Mono.fromFuture(commandGateway.send(command))
                   .then(Mono.just(Result.ok()))
                   .onErrorResume(e -> Mono.just(Result.Error(id, e.getMessage())))
                   .timeout(Duration.ofSeconds(5L));
    }

    @PostMapping("redeem/id/{id}/amount/{amount}")
    public Mono<Result> redeem(
            @PathVariable String id,
            @PathVariable int amount
    ) {
        var command = new RedeemCardCommand(id, amount);
        return Mono.fromFuture(commandGateway.send(command))
                   .then(Mono.just(Result.ok()))
                   .onErrorResume(e -> Mono.just(Result.Error(id, e.getMessage())))
                   .timeout(Duration.ofSeconds(5L));
    }

    @GetMapping(path = "bulkissue/number/{number}/amount/{amount}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<Result> bulkIssue(
            @PathVariable int number,
            @PathVariable int amount
    ) {
        return Flux.range(0, number).flatMap(i -> issue(amount));
    }

    private Mono<Result> issue(int amount) {
        String id = UUID.randomUUID().toString().substring(0, 11).toUpperCase();
        var command = new IssueCardCommand(id, amount);
        return Mono.fromFuture(commandGateway.send(command))
                   .then(Mono.just(Result.ok()))
                   .onErrorResume(e -> Mono.just(Result.Error(id, e.getMessage())))
                   .timeout(Duration.ofSeconds(10L));
    }


    private Flux<CardSummary> summerySubscription(int limit) {
        var query = new FetchCardSummariesQuery(limit);
        SubscriptionQueryResult<List<CardSummary>, CardSummary> result = queryGateway.subscriptionQuery(
                query,
                ResponseTypes.multipleInstancesOf(CardSummary.class),
                ResponseTypes.instanceOf(CardSummary.class));
        return result.initialResult()
                     .flatMapMany(Flux::fromIterable)
                     .concatWith(result.updates())
                     .doFinally(signal -> result.close());
    }

    private Flux<CountCardSummariesResponse> countSubscription() {
        var query = new CountCardSummariesQuery();
        SubscriptionQueryResult<CountCardSummariesResponse, CountCardSummariesResponse> result = queryGateway.subscriptionQuery(
                query,
                ResponseTypes.instanceOf(CountCardSummariesResponse.class),
                ResponseTypes.instanceOf(CountCardSummariesResponse.class));
        return result.initialResult()
                     .concatWith(result.updates())
                     .doFinally(signal -> result.close());
    }
}