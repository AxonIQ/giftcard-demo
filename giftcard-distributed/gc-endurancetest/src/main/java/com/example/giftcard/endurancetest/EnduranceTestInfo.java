package com.example.giftcard.endurancetest;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Milan Savic
 */
public class EnduranceTestInfo {

    private final AtomicLong startedCommands;
    private final AtomicLong failedCommands;

    public EnduranceTestInfo() {
        this.startedCommands = new AtomicLong();
        this.failedCommands = new AtomicLong();
    }

    public long getStartedCommands() {
        return startedCommands.get();
    }

    public long getFailedCommands() {
        return failedCommands.get();
    }

    public void reset() {
        startedCommands.set(0L);
        failedCommands.set(0L);
    }

    public long commandStarted() {
        return startedCommands.incrementAndGet();
    }

    public void commandFailed() {
        failedCommands.incrementAndGet();
    }
}
