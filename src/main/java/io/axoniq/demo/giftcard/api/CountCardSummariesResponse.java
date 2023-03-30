package io.axoniq.demo.giftcard.api;

import java.time.Instant;

public record CountCardSummariesResponse(
        int count,
        Instant lastEvent
) {

}
