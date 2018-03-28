package com.example.giftcard.endurancetest;

import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
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
import java.time.OffsetDateTime;
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
import javax.annotation.PreDestroy;

import static com.codahale.metrics.MetricRegistry.name;
import static java.lang.String.format;

/**
 * Performs endurance test. Test case consists of sending an {@link IssueCmd}, 10 {@link RedeemCmd}s and a {@link
 * CancelCmd}. All commands are sent one after another with random delay (maxDelay is provided at {@link
 * GcEnduranceTest#start}). After test case completes, new one is scheduled with random delay. It is also possible to
 * define duration of the test. At any time, test can be stopped using {@link GcEnduranceTest#stop()}.
 *
 * @author Milan Savic
 */
@Component
public class GcEnduranceTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private final CommandGateway commandGateway;
    private ScheduledExecutorService scheduledExecutorService;
    private final EnduranceTestInfoImpl enduranceTestInfo;

    /**
     * Instantiates the endurance test.
     *
     * @param commandGateway used for sending the commands
     * @param metrics        metrics registry used to track test metrics
     */
    public GcEnduranceTest(CommandGateway commandGateway, MetricRegistry metrics) {
        this.commandGateway = commandGateway;
        scheduledExecutorService = createScheduledExecutorService();
        enduranceTestInfo = new EnduranceTestInfoImpl(metrics);
    }

    /**
     * Starts the test execution.
     *
     * @param parallelism      maximum of parallel executions of test cases
     * @param maxDelayInMillis maximum delay between test case steps (and test cases)
     * @param duration         for how long test will run
     * @param durationTimeUnit time unit of duration
     */
    public void start(int parallelism, int maxDelayInMillis, int duration, TimeUnit durationTimeUnit) {
        start(parallelism, maxDelayInMillis);
        LOGGER.info("Duration of the test: {} {}.", duration, durationTimeUnit);
        scheduledExecutorService.schedule(this::stop, duration, durationTimeUnit);
    }

    /**
     * Starts the test execution.
     *
     * @param parallelism      maximum of parallel executions of test cases
     * @param maxDelayInMillis maximum delay between test case steps (and test cases)
     */
    public synchronized void start(int parallelism, int maxDelayInMillis) {
        enduranceTestInfo.testStarted();
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

    /**
     * Stops the test execution - no new commands and test cases will be scheduled.
     */
    @PreDestroy
    public synchronized void stop() {
        LOGGER.info("Stopping execution of endurance test.");
        scheduledExecutorService.shutdown();
        LOGGER.info("Endurance test stopped.");
    }

    /**
     * Gets the information about the test execution process.
     *
     * @return information about the test execution process
     */
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
                                         maxDelayInMillis,
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
        private final int maxDelayInMillis;
        private final Runnable onDone;

        private TestCase(String id, int maxDelayInMillis, Runnable onDone, List<?> commands) {
            this.id = id;
            this.maxDelayInMillis = maxDelayInMillis;
            this.onDone = onDone;
            this.commands = Lists.newLinkedList(commands);
        }

        private void process() {
            if (commands.isEmpty()) {
                done();
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
                done();
            }
        }

        private void done() {
            if (onDone != null) {
                onDone.run();
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

        private final Meter startedTestCases;
        private final Meter successfulCommands;
        private final Meter numberOfFailedCommands;
        private final CopyOnWriteArrayList<FailedCommandInfo<?>> failedCommands;
        private final CopyOnWriteArrayList<ExceptionInfo> exceptions;
        private long testStartTime;

        private EnduranceTestInfoImpl(MetricRegistry metrics) {
            startedTestCases = metrics.meter(name(EnduranceTestInfo.class, "started-test-cases-meter"));
            successfulCommands = metrics.meter(name(EnduranceTestInfo.class, "successful-commands-meter"));
            numberOfFailedCommands = metrics.meter(name(EnduranceTestInfo.class, "failed-commands-meter"));
            failedCommands = new CopyOnWriteArrayList<>();
            exceptions = new CopyOnWriteArrayList<>();
        }

        private void testStarted() {
            testStartTime = System.currentTimeMillis();
        }

        private long testCaseStarted() {
            startedTestCases.mark();
            return startedTestCases.getCount();
        }

        private void commandFailed(CommandMessage<?> command, Throwable cause) {
            numberOfFailedCommands.mark();
            failedCommands.add(new FailedCommandInfo<>(OffsetDateTime.now(), command, cause));
            if (failedCommands.size() + exceptions.size() > EXCEPTIONS_THRESHOLD) {
                failedCommands.remove(0);
            }
        }

        private void commandSucceeded() {
            successfulCommands.mark();
        }

        private void exception(Throwable t) {
            exceptions.add(new ExceptionInfo(OffsetDateTime.now(), t));
            if (failedCommands.size() + exceptions.size() > EXCEPTIONS_THRESHOLD) {
                exceptions.remove(0);
            }
        }

        @Override
        public long getStartedTestCases() {
            return startedTestCases.getCount();
        }

        @Override
        public long getSuccessfulCommands() {
            return successfulCommands.getCount();
        }

        @Override
        public double getSuccessfulCommandsOneMinuteRate() {
            return successfulCommands.getOneMinuteRate();
        }

        @Override
        public List<FailedCommandInfo<?>> getFailedCommands() {
            return Collections.unmodifiableList(failedCommands);
        }

        @Override
        public long getNumberOfFailedCommands() {
            return numberOfFailedCommands.getCount();
        }

        @Override
        public double getNumberOfFailedCommandsOneMinuteRate() {
            return numberOfFailedCommands.getOneMinuteRate();
        }

        @Override
        public List<ExceptionInfo> getExceptions() {
            return Collections.unmodifiableList(exceptions);
        }

        @Override
        public long getTestDuration() {
            if (testStartTime == 0) {
                return 0;
            }
            return System.currentTimeMillis() - testStartTime;
        }
    }
}
