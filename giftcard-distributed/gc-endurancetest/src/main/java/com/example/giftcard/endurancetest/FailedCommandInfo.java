package com.example.giftcard.endurancetest;

import org.axonframework.commandhandling.CommandMessage;

/**
 * @author Milan Savic
 */
public class FailedCommandInfo<T> {

    private final CommandMessage<T> command;
    private final Throwable cause;

    public FailedCommandInfo(CommandMessage<T> command, Throwable cause) {
        this.command = command;
        this.cause = cause;
    }

    public CommandMessage<T> getCommand() {
        return command;
    }

    public Throwable getCause() {
        return cause;
    }
}
