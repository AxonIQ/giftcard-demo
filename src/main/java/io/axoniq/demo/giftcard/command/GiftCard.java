package io.axoniq.demo.giftcard.command;

import io.axoniq.demo.giftcard.api.CancelCommand;
import io.axoniq.demo.giftcard.api.CancelEvent;
import io.axoniq.demo.giftcard.api.IssueCommand;
import io.axoniq.demo.giftcard.api.IssuedEvent;
import io.axoniq.demo.giftcard.api.RedeemCommand;
import io.axoniq.demo.giftcard.api.RedeemedEvent;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.springframework.context.annotation.Profile;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Profile("command")
@Aggregate(cache = "giftCardCache")
public class GiftCard {

    @AggregateIdentifier
    private String giftCardId;
    private int remainingValue;

    @CommandHandler
    public GiftCard(IssueCommand command) {
        if (command.getAmount() <= 0) {
            throw new IllegalArgumentException("amount <= 0");
        }
        apply(new IssuedEvent(command.getId(), command.getAmount()));
    }

    @CommandHandler
    public void handle(RedeemCommand command) {
        if (command.getAmount() <= 0) {
            throw new IllegalArgumentException("amount <= 0");
        }
        if (command.getAmount() > remainingValue) {
            throw new IllegalStateException("amount > remaining value");
        }
        apply(new RedeemedEvent(giftCardId, command.getAmount()));
    }

    @CommandHandler
    public void handle(CancelCommand command) {
        apply(new CancelEvent(giftCardId));
    }

    @EventSourcingHandler
    public void on(IssuedEvent event) {
        giftCardId = event.getId();
        remainingValue = event.getAmount();
    }

    @EventSourcingHandler
    public void on(RedeemedEvent event) {
        remainingValue -= event.getAmount();
    }

    @EventSourcingHandler
    public void on(CancelEvent event) {
        remainingValue = 0;
    }

    public GiftCard() {
        // Required by Axon to construct an empty instance to initiate Event Sourcing.
    }
}

