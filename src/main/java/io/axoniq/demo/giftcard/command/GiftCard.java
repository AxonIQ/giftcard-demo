package io.axoniq.demo.giftcard.command;

import io.axoniq.demo.giftcard.api.CancelCardCommand;
import io.axoniq.demo.giftcard.api.CardCanceledEvent;
import io.axoniq.demo.giftcard.api.CardIssuedEvent;
import io.axoniq.demo.giftcard.api.CardRedeemedEvent;
import io.axoniq.demo.giftcard.api.IssueCardCommand;
import io.axoniq.demo.giftcard.api.RedeemCardCommand;
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

    // Tag this handler to use it as code sample in the documentation
    // tag::IssueCardCommandHandler[]
    @CommandHandler
    public GiftCard(IssueCardCommand command) {
        if (command.amount() <= 0) {
            throw new IllegalArgumentException("amount <= 0");
        }
        apply(new CardIssuedEvent(command.id(), command.amount()));
    }
    // end::IssueCardCommandHandler[]

    // Tag this handler to use it as code sample in the documentation
    // tag::RedeemCardCommandHandler[]
    @CommandHandler
    public void handle(RedeemCardCommand command) {
        if (command.amount() <= 0) {
            throw new IllegalArgumentException("amount <= 0");
        }
        if (command.amount() > remainingValue) {
            throw new IllegalStateException("amount > remaining value");
        }
        apply(new CardRedeemedEvent(giftCardId, command.amount()));
    }
    // end::RedeemCardCommandHandler[]

    @SuppressWarnings("unused")
    @CommandHandler
    public void handle(CancelCardCommand command) {
        apply(new CardCanceledEvent(giftCardId));
    }

    @EventSourcingHandler
    public void on(CardIssuedEvent event) {
        giftCardId = event.id();
        remainingValue = event.amount();
    }

    @EventSourcingHandler
    public void on(CardRedeemedEvent event) {
        remainingValue -= event.amount();
    }

    @EventSourcingHandler
    public void on(CardCanceledEvent event) {
        remainingValue = 0;
    }

    public GiftCard() {
        // Required by Axon to construct an empty instance to initiate Event Sourcing.
    }
}

