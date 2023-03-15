package io.axoniq.demo.giftcard.query;

import io.axoniq.demo.giftcard.api.query.CardSummary;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document
public record CardEntity(
        @Id String id,
        int initialValue,
        int remainingValue,
        Instant issued,
        Instant lastUpdated
) {

    public static CardEntity issue(String id, int initialValue, Instant issuedAt) {
        return new CardEntity(id, initialValue, initialValue, issuedAt, issuedAt);
    }

    public CardEntity redeem(int amount, Instant redeemedAt) {
        return new CardEntity(
                this.id,
                this.initialValue,
                this.remainingValue - amount,
                this.issued,
                redeemedAt);
    }

    public CardEntity cancel(Instant cancelledAt) {
        return new CardEntity(
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
