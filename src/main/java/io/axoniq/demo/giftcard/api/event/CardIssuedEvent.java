package io.axoniq.demo.giftcard.api.event;

public record CardIssuedEvent(
        String id,
        int amount
) {

}
