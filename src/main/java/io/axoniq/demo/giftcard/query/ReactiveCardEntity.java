package io.axoniq.demo.giftcard.query;

import io.axoniq.demo.giftcard.api.query.CardSummary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document
public record ReactiveCardEntity(
        @Id String id,
        int initialValue,
        int remainingValue,
        Instant issued,
        Instant lastUpdated
) {

    public static ReactiveCardEntity issue(String id, int initialValue, Instant issuedAt) {
        return new ReactiveCardEntity(id, initialValue, initialValue, issuedAt, issuedAt);
    }

    public ReactiveCardEntity redeem(int amount, Instant redeemedAt) {
        return new ReactiveCardEntity(
                this.id,
                this.initialValue,
                this.remainingValue - amount,
                this.issued,
                redeemedAt);
    }

    public ReactiveCardEntity cancel(Instant cancelledAt) {
        return new ReactiveCardEntity(
                this.id,
                this.initialValue,
                0,
                this.issued,
                cancelledAt);
    }

    public CardSummary toSummary() {
        return new CardSummary(id, initialValue, remainingValue, issued, lastUpdated);
    }
}
