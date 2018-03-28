package com.example.giftcard.endurancetest.web;

import com.codahale.metrics.MetricRegistry;
import com.example.giftcard.endurancetest.GcEnduranceTest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * Rest API for the endurance test.
 *
 * @author Milan Savic
 */
@RestController
public class GcEnduranceTestController {

    private final GcEnduranceTest gcEnduranceTest;
    private final MetricRegistry metricRegistry;

    /**
     * Instantiates the controller with the reference to the endurance test.
     *
     * @param gcEnduranceTest endurance test
     * @param metricRegistry  contains information about test metrics
     */
    public GcEnduranceTestController(GcEnduranceTest gcEnduranceTest,
                                     MetricRegistry metricRegistry) {
        this.gcEnduranceTest = gcEnduranceTest;
        this.metricRegistry = metricRegistry;
    }

    /**
     * Starts the test execution.
     *
     * @param parallelism      maximum of parallel executions of test cases
     * @param maxDelayInMillis maximum delay between test case steps (and test cases)
     */
    @GetMapping("/start")
    public void start(int parallelism, int maxDelayInMillis) {
        gcEnduranceTest.start(parallelism, maxDelayInMillis);
    }

    /**
     * Starts the test execution.
     *
     * @param parallelism      maximum of parallel executions of test cases
     * @param maxDelayInMillis maximum delay between test case steps (and test cases)
     * @param duration         for how long test will run
     * @param durationTimeUnit time unit of duration
     */
    @GetMapping("/startWithTimeLimit")
    public void start(int parallelism, int maxDelayInMillis, int duration, TimeUnit durationTimeUnit) {
        gcEnduranceTest.start(parallelism, maxDelayInMillis, duration, durationTimeUnit);
    }

    /**
     * Stops the test execution.
     */
    @GetMapping("/stop")
    public void stop() {
        gcEnduranceTest.stop();
    }

    /**
     * Gets metrics regarding test execution.
     *
     * @return metrics regarding execution
     */
    @GetMapping("/metrics")
    public MetricRegistry getMetrics() {
        return metricRegistry;
    }
}
