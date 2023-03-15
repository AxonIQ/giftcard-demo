package io.axoniq.demo.giftcard.api;

import java.time.Instant;

public record CardSummary(
        String id,
        int initialValue,
        int remainingValue,
        Instant issued,
        Instant lastUpdated
) {
}
