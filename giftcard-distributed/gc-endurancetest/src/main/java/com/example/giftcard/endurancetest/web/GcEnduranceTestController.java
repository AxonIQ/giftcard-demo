package com.example.giftcard.endurancetest.web;

import com.example.giftcard.endurancetest.GcEnduranceTest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

/**
 * @author Milan Savic
 */
@RestController("/api")
public class GcEnduranceTestController {

    private final GcEnduranceTest gcEnduranceTest;

    public GcEnduranceTestController(GcEnduranceTest gcEnduranceTest) {
        this.gcEnduranceTest = gcEnduranceTest;
    }

    @GetMapping("/start")
    public void start(int parallelism, int maxDelayInMilis) {
        gcEnduranceTest.start(parallelism, maxDelayInMilis, TimeUnit.MILLISECONDS);
    }

    @GetMapping("/stop")
    public void stop() {
        gcEnduranceTest.stop();
    }
}
