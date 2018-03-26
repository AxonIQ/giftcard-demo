package com.example.giftcard.endurancetest;

import com.example.giftcard.api.CancelCmd;
import com.example.giftcard.api.IssueCmd;
import com.example.giftcard.api.RedeemCmd;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javax.annotation.PreDestroy;

/**
 * @author Milan Savic
 */
@Component
public class GcEnduranceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(GcEnduranceTest.class);

    private final CommandGateway commandGateway;
    private ScheduledExecutorService scheduledExecutorService;

    public GcEnduranceTest(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
        scheduledExecutorService = createScheduledExecutorService();
    }

    public synchronized void start(int parallelism, int maxDelay, TimeUnit unit) {
        LOGGER.info("Started execution of endurance test. Parameters: parallelism {}, maxDelay {}, unit {}.",
                    parallelism,
                    maxDelay,
                    unit);
        if (scheduledExecutorService.isTerminated()) {
            LOGGER.info("Scheduler is terminated, starting new one...");
            scheduledExecutorService = createScheduledExecutorService();
        }
        for (int i = 0; i < parallelism; i++) {
            scheduledExecutorService.schedule(() -> performTestCase(maxDelay, unit),
                                              rand(maxDelay),
                                              unit);
        }
    }

    public synchronized void stop() {
        LOGGER.info("Stopping execution of endurance test.");
        shutDown();
        LOGGER.info("Endurance test stopped.");
    }

    @PreDestroy
    public void shutDown() {
        try {
            LOGGER.info("Shutting down the scheduler...");
            scheduledExecutorService.awaitTermination(5, TimeUnit.SECONDS);
            LOGGER.info("Scheduler is shut down.");
        } catch (InterruptedException e) {
            LOGGER.warn("Error happened while trying to shut down the scheduler.", e);
            Thread.currentThread().interrupt();
        }
    }

    private void performTestCase(int maxDelay, TimeUnit unit) {
        String id = UUID.randomUUID().toString();
        try {
            commandGateway.sendAndWait(new IssueCmd(id, 100));
            for (int i = 0; i < 10; i++) {
                commandGateway.sendAndWait(new RedeemCmd(id, 100));
            }
            commandGateway.sendAndWait(new CancelCmd(id));
        } catch (Exception e) {
            // we don't want to stop endurance test if one test case failed
            LOGGER.error("Unexpected error occurred during performing a test case.", e);
        }
        scheduledExecutorService.schedule(() -> performTestCase(maxDelay, unit),
                                          rand(maxDelay),
                                          unit);
    }

    private int rand(int max) {
        return new Random().nextInt(max);
    }

    private ScheduledExecutorService createScheduledExecutorService() {
        return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    }
}
