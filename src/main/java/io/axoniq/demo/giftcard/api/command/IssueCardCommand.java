package io.axoniq.demo.giftcard.api.command;

import org.axonframework.modelling.command.TargetAggregateIdentifier;

public record IssueCardCommand(
        @TargetAggregateIdentifier String id,
        int amount
) {

}
