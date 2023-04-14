package io.axoniq.demo.giftcard.api.query;

import java.time.Instant;

public record CountCardSummariesResponse(
        int count,
        Instant lastEvent
) {

}
