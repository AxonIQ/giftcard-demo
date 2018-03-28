package com.example.giftcard.endurancetest;

import java.util.List;

/**
 * Information about endurance test info progress.
 *
 * @author Milan Savic
 */
public interface EnduranceTestInfo {

    /**
     * How many test cases have started.
     *
     * @return number of started test cases
     */
    long getStartedTestCases();

    /**
     * How many commands succeeded.
     *
     * @return number of successful commands
     */
    long getSuccessfulCommands();

    /**
     * Information about failed commands. These are limited to 1000 together with exceptions.
     *
     * @return information about failed commands
     */
    List<FailedCommandInfo<?>> getFailedCommands();

    /**
     * Gets total number of failed commands.
     *
     * @return number of failed commands
     */
    long getNumberOfFailedCommands();

    /**
     * List of exceptions which occurred during test execution. These are limited to 1000 together with failed commands.
     *
     * @return list of exceptions
     */
    List<Throwable> getExceptions();
}
