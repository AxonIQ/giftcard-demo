package io.axoniq.demo.giftcard.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record CancelCardCommand(
        @TargetAggregateIdentifier String id
) {

}
