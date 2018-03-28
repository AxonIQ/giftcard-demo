package com.example.giftcard.endurancetest;

import org.axonframework.commandhandling.CommandMessage;

import java.time.OffsetDateTime;

/**
 * Information about failed command.
 *
 * @author Milan Savic
 */
public class FailedCommandInfo<T> extends ExceptionInfo {

    private final CommandMessage<T> command;

    /**
     * Instantiates failed command info with command message which failed ant the cause why it failed.
     *
     * @param timestamp the timestamp when command failed
     * @param command   the command which failed
     * @param cause     the cause why command failed
     */
    public FailedCommandInfo(OffsetDateTime timestamp, CommandMessage<T> command, Throwable cause) {
        super(timestamp, cause);
        this.command = command;
    }

    /**
     * Gets the command message which failed.
     *
     * @return the command message which failed
     */
    public CommandMessage<T> getCommand() {
        return command;
    }

    @Override
    public String toString() {
        return getTimestamp() + ": " + getCommand().getCommandName() + " -> " + getCause().getMessage();
    }
}
