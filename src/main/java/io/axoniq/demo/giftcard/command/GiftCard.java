package io.axoniq.demo.giftcard.command;

import io.axoniq.demo.giftcard.api.*;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.commandhandling.model.AggregateIdentifier;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.lang.invoke.MethodHandles;

import static org.axonframework.commandhandling.model.AggregateLifecycle.apply;

@Aggregate
@Profile("command")
public class GiftCard {

    private final static Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String id;
    private int remainingValue;

    public GiftCard() {
        log.debug("empty constructor invoked");
    }

    @CommandHandler
    public GiftCard(IssueCmd cmd) {
        log.debug("handling {}", cmd);
        if(cmd.getAmount() <= 0) throw new IllegalArgumentException("amount <= 0");
        apply(new IssuedEvt(cmd.getId(), cmd.getAmount()));
    }

    @CommandHandler
    public void handle(RedeemCmd cmd) {
        log.debug("handling {}", cmd);
        if(cmd.getAmount() <= 0) throw new IllegalArgumentException("amount <= 0");
        if(cmd.getAmount() > remainingValue) throw new IllegalStateException("amount > remaining value");
        apply(new RedeemedEvt(id, cmd.getAmount()));
    }

    @CommandHandler
    public void handle(CancelCmd cmd) {
        log.debug("handling {}", cmd);
        apply(new CancelEvt(id));
    }

    @EventSourcingHandler
    public void on(IssuedEvt evt) {
        log.debug("applying {}", evt);
        id = evt.getId();
        remainingValue = evt.getAmount();
        log.debug("new remaining value: {}", remainingValue);
    }

    @EventSourcingHandler
    public void on(RedeemedEvt evt) {
        log.debug("applying {}", evt);
        remainingValue -= evt.getAmount();
        log.debug("new remaining value: {}", remainingValue);
    }

    @EventSourcingHandler
    public void on(CancelEvt evt) {
        log.debug("applying {}", evt);
        remainingValue = 0;
        log.debug("new remaining value: {}", remainingValue);
    }
}
