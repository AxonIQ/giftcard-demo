package com.example.giftcard.endurancetest;

import com.example.giftcard.api.CancelCmd;
import com.example.giftcard.api.IssueCmd;
import com.example.giftcard.api.RedeemCmd;
import com.google.common.collect.Lists;
import org.axonframework.commandhandling.CommandCallback;
import org.axonframework.commandhandling.CommandMessage;
import org.axonframework.commandhandling.gateway.CommandGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandles;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
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

        List<?> commands = Arrays.asList(new IssueCmd(id, 100),
                                         new RedeemCmd(id, 9),
                                         new RedeemCmd(id, 9),
                                         new RedeemCmd(id, 9),
                                         new RedeemCmd(id, 9),
                                         new RedeemCmd(id, 9),
                                         new RedeemCmd(id, 9),
                                         new RedeemCmd(id, 9),
                                         new RedeemCmd(id, 9),
                                         new RedeemCmd(id, 9),
                                         new RedeemCmd(id, 9),
                                         new CancelCmd(id));

        TestCase testCase = new TestCase(id,
                                         commandGateway,
                                         scheduledExecutorService,
                                         maxDelayInMillis,
                                         enduranceTestInfo,
                                         () -> scheduledExecutorService
                                                 .schedule(() -> performTestCase(maxDelayInMillis),
                                                           rand(maxDelayInMillis),
                                                           TimeUnit.MILLISECONDS),
                                         commands);
        testCase.process();
    }

    private int rand(int max) {
        return new Random().nextInt(max);
    }

    private ScheduledExecutorService createScheduledExecutorService() {
        return Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    }

    private class TestCase {

        private final String id;
        private final Queue<?> commands;
        private final CommandGateway commandGateway;
        private final ScheduledExecutorService scheduledExecutorService;
        private final int maxDelayInMillis;
        private final EnduranceTestInfoImpl enduranceTestInfo;
        private final Runnable onDone;

        private TestCase(String id, CommandGateway commandGateway,
                         ScheduledExecutorService scheduledExecutorService,
                         int maxDelayInMillis,
                         EnduranceTestInfoImpl enduranceTestInfo,
                         Runnable onDone, List<?> commands) {
            this.id = id;
            this.commandGateway = commandGateway;
            this.scheduledExecutorService = scheduledExecutorService;
            this.maxDelayInMillis = maxDelayInMillis;
            this.enduranceTestInfo = enduranceTestInfo;
            this.onDone = onDone;
            this.commands = Lists.newLinkedList(commands);
        }

        private void process() {
            if (commands.isEmpty()) {
                onDone.run();
                return;
            }

            Object command = commands.poll();
            try {
                commandGateway.send(command, new MonitorCommandCallback<>(enduranceTestInfo, () ->
                        scheduledExecutorService
                                .schedule(this::process, rand(maxDelayInMillis), TimeUnit.MILLISECONDS)));
            } catch (Exception e) {
                LOGGER.error(format("Unexpected error occurred during processing test case with id: %s", id), e);
                enduranceTestInfo.exception(e);
            }
        }
    }

    private class MonitorCommandCallback<T> implements CommandCallback<T, Object> {

        private final EnduranceTestInfoImpl enduranceTestInfo;
        private final Runnable nextStep;

        private MonitorCommandCallback(EnduranceTestInfoImpl enduranceTestInfo, Runnable nextStep) {
            this.enduranceTestInfo = enduranceTestInfo;
            this.nextStep = nextStep;
        }

        @Override
        public void onSuccess(CommandMessage<? extends T> commandMessage, Object result) {
            LOGGER.info("Command {} succeeded.", commandMessage.getCommandName());
            enduranceTestInfo.commandSucceeded();
            runNextStep();
        }

        @Override
        public void onFailure(CommandMessage<? extends T> commandMessage, Throwable cause) {
            LOGGER.warn(format("Command %s failed.", commandMessage.getCommandName()), cause);
            enduranceTestInfo.commandFailed(commandMessage, cause);
            runNextStep();
        }

        private void runNextStep() {
            if (nextStep != null) {
                nextStep.run();
            }
        }
    }

    private class EnduranceTestInfoImpl implements EnduranceTestInfo {

        private static final int EXCEPTIONS_THRESHOLD = 1000;

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
            if (failedCommands.size() + exceptions.size() > EXCEPTIONS_THRESHOLD) {
                failedCommands.remove(0);
            }
        }

        private void commandSucceeded() {
            successfulCommands.incrementAndGet();
        }

        private void exception(Throwable t) {
            exceptions.add(t);
            if (failedCommands.size() + exceptions.size() > EXCEPTIONS_THRESHOLD) {
                exceptions.remove(0);
            }
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
