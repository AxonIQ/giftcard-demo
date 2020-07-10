package io.axoniq.demo.giftcard.command;

import io.axoniq.demo.giftcard.api.CancelCmd;
import io.axoniq.demo.giftcard.api.CancelEvt;
import io.axoniq.demo.giftcard.api.IssueCmd;
import io.axoniq.demo.giftcard.api.IssuedEvt;
import io.axoniq.demo.giftcard.api.RedeemCmd;
import io.axoniq.demo.giftcard.api.RedeemedEvt;
import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.spring.stereotype.Aggregate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;

import java.lang.invoke.MethodHandles;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;

@Aggregate
@Profile("command")
public class GiftCard {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @AggregateIdentifier
    private String giftCardId;
    private int remainingValue;

    @CommandHandler
    public GiftCard(IssueCmd cmd) {
        logger.debug("handling {}", cmd);
        if (cmd.getAmount() <= 0) {
            throw new IllegalArgumentException("amount <= 0");
        }
        apply(new IssuedEvt(cmd.getId(), cmd.getAmount()));
    }

    @CommandHandler
    public void handle(RedeemCmd cmd) {
        logger.debug("handling {}", cmd);
        if (cmd.getAmount() <= 0) {
            throw new IllegalArgumentException("amount <= 0");
        }
        if (cmd.getAmount() > remainingValue) {
            throw new IllegalStateException("amount > remaining value");
        }
        apply(new RedeemedEvt(giftCardId, cmd.getAmount()));
    }

    @CommandHandler
    public void handle(CancelCmd cmd) {
        logger.debug("handling {}", cmd);
        apply(new CancelEvt(giftCardId));
    }

    @EventSourcingHandler
    public void on(IssuedEvt evt) {
        logger.debug("applying {}", evt);
        giftCardId = evt.getId();
        remainingValue = evt.getAmount();
        logger.debug("new remaining value: {}", remainingValue);
    }

    @EventSourcingHandler
    public void on(RedeemedEvt evt) {
        logger.debug("applying {}", evt);
        remainingValue -= evt.getAmount();
        logger.debug("new remaining value: {}", remainingValue);
    }

    @EventSourcingHandler
    public void on(CancelEvt evt) {
        logger.debug("applying {}", evt);
        remainingValue = 0;
        logger.debug("new remaining value: {}", remainingValue);
    }

    public GiftCard() {
        // Required by Axon
        logger.debug("Empty constructor invoked");
    }
}
