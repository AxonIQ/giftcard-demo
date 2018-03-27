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
     * Information about failed commands.
     *
     * @return information about failed commands
     */
    List<FailedCommandInfo<?>> getFailedCommands();

    /**
     * List of exceptions which occurred during test execution.
     *
     * @return list of exceptions
     */
    List<Throwable> getExceptions();
}
