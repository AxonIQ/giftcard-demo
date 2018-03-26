package com.example.giftcard.endurancetest;

import com.example.giftcard.api.CancelCmd;
import com.example.giftcard.api.IssueCmd;
import com.example.giftcard.api.RedeemCmd;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.annotation.PreDestroy;

import static java.lang.String.format;

/**
 * @author Milan Savic
 */
@Component
public class GcEnduranceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CommandGateway commandGateway;
    private ScheduledExecutorService scheduledExecutorService;
    private final AtomicLong counter;

    public GcEnduranceTest(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
        scheduledExecutorService = createScheduledExecutorService();
        counter = new AtomicLong();
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
        counter.set(0L);
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
        LOGGER.info("Shutting down the scheduler...");
        scheduledExecutorService.shutdown();
        LOGGER.info("Scheduler is shut down.");
    }

    private void performTestCase(int maxDelay, TimeUnit unit) {
        String id = UUID.randomUUID().toString();
        long currentCount = counter.incrementAndGet();
        LOGGER.info("Executing test case #{} with id: {}.", currentCount, id);
        try {
            commandGateway.sendAndWait(new IssueCmd(id, 100));
            for (int i = 0; i < 10; i++) {
                commandGateway.sendAndWait(new RedeemCmd(id, 9));
            }
            commandGateway.sendAndWait(new CancelCmd(id));
        } catch (Exception e) {
            // we don't want to stop endurance test if one test case failed
            LOGGER.error(format("Unexpected error occurred during performing a test case with id: %s.", id), e);
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
