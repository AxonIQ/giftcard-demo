package com.example.giftcard.endurancetest;

import java.time.OffsetDateTime;

/**
 * @author Milan Savic
 */
public class ExceptionInfo {

    private final OffsetDateTime timestamp;
    private final Throwable cause;

    /**
     * Instantiates failed command info with command message which failed ant the cause why it failed.
     *
     * @param timestamp the timestamp when command failed
     * @param cause     the cause why command failed
     */
    public ExceptionInfo(OffsetDateTime timestamp, Throwable cause) {
        this.timestamp = timestamp;
        this.cause = cause;
    }

    /**
     * Gets the timestamp when exception occurred.
     *
     * @return the timestamp when exception occurred
     */
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    /**
     * Gets the cause why command failed.
     *
     * @return the cause why command failed
     */
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        return getTimestamp() + ": " + getCause().getMessage();
    }
}
