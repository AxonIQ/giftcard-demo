package io.axoniq.demo.giftcard.gui;

import io.axoniq.demo.giftcard.api.IssueCardCommand;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class BulkIssuer {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final AtomicInteger success = new AtomicInteger();
    private final AtomicInteger error = new AtomicInteger();
    private final AtomicInteger remaining = new AtomicInteger();

    public BulkIssuer(CommandGateway commandGateway,
                      int number,
                      int amount,
                      Consumer<BulkIssuer> callback) {
        remaining.set(number);
        new Thread(() -> {
            for (int i = 0; i < number; i++) {
                String id = UUID.randomUUID().toString().substring(0, 11).toUpperCase();
                commandGateway.send(new IssueCardCommand(id, amount))
                              .whenComplete((Object result, Throwable throwable) -> {
                                  if (throwable == null) {
                                      success.incrementAndGet();
                                  } else {
                                      error.incrementAndGet();
                                  }
                                  remaining.decrementAndGet();
                              });
            }
        }).start();
        new Thread(() -> {
            try {
                while (remaining.get() > 0) {
                    callback.accept(this);
                    Thread.sleep(1000);
                }
                callback.accept(this);
            } catch (InterruptedException ex) {
                logger.error("Interrupted", ex);
            }
        }).start();
    }

    public AtomicInteger getSuccess() {
        return success;
    }

    public AtomicInteger getError() {
        return error;
    }

    public AtomicInteger getRemaining() {
        return remaining;
    }
}
