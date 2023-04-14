package io.axoniq.demo.giftcard.api;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

// Tag this command to use it as code sample in the documentation
// tag::CancelCardCommand[]
public record CancelCardCommand(
        @TargetAggregateIdentifier String id
) {

}
// end::CancelCardCommand[]
