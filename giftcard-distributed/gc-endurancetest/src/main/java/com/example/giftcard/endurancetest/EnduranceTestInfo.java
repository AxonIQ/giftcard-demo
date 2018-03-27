package com.example.giftcard.endurancetest;

import java.util.List;

/**
 * @author Milan Savic
 */
public interface EnduranceTestInfo {

    long getStartedTestCases();
    long getSuccessfulCommands();
    List<FailedCommandInfo<?>> getFailedCommands();
    List<Throwable> getExceptions();
}
