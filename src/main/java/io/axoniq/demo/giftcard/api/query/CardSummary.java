package io.axoniq.demo.giftcard.api.query;

public record CardSummary(
        String id,
        int initialValue,
        int remainingValue
) {
    public CardSummary redeem(int amount){
        return new CardSummary(this.id, this.initialValue, this.remainingValue - amount);
    }
}
