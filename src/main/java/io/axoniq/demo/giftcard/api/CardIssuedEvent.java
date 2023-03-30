package io.axoniq.demo.giftcard.api;

public record CardIssuedEvent(
        String id,
        int amount
) {

}
