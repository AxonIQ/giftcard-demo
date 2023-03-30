package io.axoniq.demo.giftcard.api;

import java.time.Instant;

public record CardSummary(
        String id,
        int initialValue,
        int remainingValue,
        Instant issued,
        Instant lastUpdated
) {

    public static CardSummary issue(String id, int initialValue, Instant issuedAt) {
        return new CardSummary(id, initialValue, initialValue, issuedAt, issuedAt);
    }

    public CardSummary redeem(int amount, Instant redeemedAt) {
        return new CardSummary(
                this.id,
                this.initialValue,
                this.remainingValue - amount,
                this.issued,
                redeemedAt);
    }

    public CardSummary cancel(Instant cancelledAt) {
        return new CardSummary(
                this.id,
                this.initialValue,
                0,
                this.issued,
                cancelledAt);
    }
}
