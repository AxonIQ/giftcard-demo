package com.example.giftcard.endurancetest;

import com.example.giftcard.api.CancelCmd;
import com.example.giftcard.api.IssueCmd;
import com.example.giftcard.api.RedeemCmd;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
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
    private final EnduranceTestInfoImpl enduranceTestInfo;

    public GcEnduranceTest(CommandGateway commandGateway) {
        this.commandGateway = commandGateway;
        scheduledExecutorService = createScheduledExecutorService();
        enduranceTestInfo = new EnduranceTestInfoImpl();
    }

    public void start(int parallelism, int maxDelayInMillis, int duration, TimeUnit durationTimeUnit) {
        start(parallelism, maxDelayInMillis);
        LOGGER.info("Duration of the test: {} {}.", duration, durationTimeUnit);
        scheduledExecutorService.schedule(this::stop, duration, durationTimeUnit);
    }

    public synchronized void start(int parallelism, int maxDelayInMillis) {
        LOGGER.info("Started execution of endurance test. Parameters: parallelism {}, maxDelayInMillis {}, unit {}.",
                    parallelism,
                    maxDelayInMillis,
                    TimeUnit.MILLISECONDS);
        if (scheduledExecutorService.isTerminated()) {
            LOGGER.info("Scheduler is terminated, starting new one...");
            scheduledExecutorService = createScheduledExecutorService();
        }
        for (int i = 0; i < parallelism; i++) {
            scheduledExecutorService.schedule(() -> performTestCase(maxDelayInMillis),
                                              rand(maxDelayInMillis),
                                              TimeUnit.MILLISECONDS);
        }
    }

    @PreDestroy
    public synchronized void stop() {
        LOGGER.info("Stopping execution of endurance test.");
        scheduledExecutorService.shutdown();
        LOGGER.info("Endurance test stopped.");
    }

    public EnduranceTestInfo getInfo() {
        return enduranceTestInfo;
    }

    private void performTestCase(int maxDelayInMillis) {
        String id = UUID.randomUUID().toString();
        LOGGER.info("Executing test case #{} with id: {}.", enduranceTestInfo.testCaseStarted(), id);

        try {
            commandGateway.send(new IssueCmd(id, 100), new MonitorCommandCallback<>(enduranceTestInfo, () ->
                    commandGateway.send(new RedeemCmd(id, 9), new MonitorCommandCallback<>(enduranceTestInfo, () ->
                            commandGateway.send(new RedeemCmd(id, 9), new MonitorCommandCallback<>(enduranceTestInfo, () ->
                                    commandGateway.send(new RedeemCmd(id, 9), new MonitorCommandCallback<>(enduranceTestInfo, () ->
                                            commandGateway.send(new RedeemCmd(id, 9), new MonitorCommandCallback<>(enduranceTestInfo, () ->
                                                    commandGateway.send(new RedeemCmd(id, 9), new MonitorCommandCallback<>(enduranceTestInfo, () ->
                                                            commandGateway.send(new CancelCmd(id), new MonitorCommandCallback<>(enduranceTestInfo, null))))))))))))));
        } catch (Exception e) {
            LOGGER.error(format("Unexpected error occurred during processing test case with id: %s", id), e);
            enduranceTestInfo.exception(e);
        }

        scheduledExecutorService.schedule(() -> performTestCase(maxDelayInMillis),
                                          rand(maxDelayInMillis),
                                          TimeUnit.MILLISECONDS);
    }

    private int rand(int max) {
        return new Random().nextInt(max);
    }

    private ScheduledExecutorService createScheduledExecutorService() {
        return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    }

    private class MonitorCommandCallback<T> implements CommandCallback<T, Object> {

        private final EnduranceTestInfoImpl enduranceTestInfo;
        private final Runnable onSuccess;

        private MonitorCommandCallback(EnduranceTestInfoImpl enduranceTestInfo, Runnable onSuccess) {
            this.enduranceTestInfo = enduranceTestInfo;
            this.onSuccess = onSuccess;
        }

        @Override
        public void onSuccess(CommandMessage<? extends T> commandMessage, Object result) {
            LOGGER.info("Command {} succeeded.", commandMessage.getCommandName());
            enduranceTestInfo.commandSucceeded();
            if (onSuccess != null) {
                onSuccess.run();
            }
        }

        @Override
        public void onFailure(CommandMessage<? extends T> commandMessage, Throwable cause) {
            LOGGER.warn(format("Command %s failed.", commandMessage.getCommandName()), cause);
            enduranceTestInfo.commandFailed(commandMessage, cause);
        }
    }

    private class EnduranceTestInfoImpl implements EnduranceTestInfo {

        private final AtomicLong startedTestCases;
        private final AtomicLong successfulCommands;
        private final CopyOnWriteArrayList<FailedCommandInfo<?>> failedCommands;
        private final CopyOnWriteArrayList<Throwable> exceptions;

        private EnduranceTestInfoImpl() {
            this.startedTestCases = new AtomicLong();
            this.successfulCommands = new AtomicLong();
            this.failedCommands = new CopyOnWriteArrayList<>();
            this.exceptions = new CopyOnWriteArrayList<>();
        }

        private long testCaseStarted() {
            return startedTestCases.incrementAndGet();
        }

        private void commandFailed(CommandMessage<?> command, Throwable cause) {
            failedCommands.add(new FailedCommandInfo<>(command, cause));
        }

        private void commandSucceeded() {
            successfulCommands.incrementAndGet();
        }

        private void exception(Throwable t) {
            exceptions.add(t);
        }

        @Override
        public long getStartedTestCases() {
            return startedTestCases.get();
        }

        @Override
        public long getSuccessfulCommands() {
            return successfulCommands.get();
        }

        @Override
        public List<FailedCommandInfo<?>> getFailedCommands() {
            return Collections.unmodifiableList(failedCommands);
        }

        @Override
        public List<Throwable> getExceptions() {
            return Collections.unmodifiableList(exceptions);
        }
    }
}
