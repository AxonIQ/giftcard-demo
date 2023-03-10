package io.axoniq.demo.giftcard.api.query;

public record CountCardSummariesResponse(
        int count,
        long lastEvent

) {

}
