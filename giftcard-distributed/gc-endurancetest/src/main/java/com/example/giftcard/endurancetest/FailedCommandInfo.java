package com.example.giftcard.endurancetest;

import org.axonframework.commandhandling.CommandMessage;

/**
 * Information about failed command.
 *
 * @author Milan Savic
 */
public class FailedCommandInfo<T> {

    private final CommandMessage<T> command;
    private final Throwable cause;

    /**
     * Instantiates failed command info with command message which failed ant the cause why it failed.
     *
     * @param command the command which failed
     * @param cause   the cause why command failed
     */
    public FailedCommandInfo(CommandMessage<T> command, Throwable cause) {
        this.command = command;
        this.cause = cause;
    }

    /**
     * Gets the command message which failed.
     *
     * @return the command message which failed
     */
    public CommandMessage<T> getCommand() {
        return command;
    }

    /**
     * Gets the cause why command failed.
     *
     * @return the cause why command failed
     */
    public Throwable getCause() {
        return cause;
    }
}
