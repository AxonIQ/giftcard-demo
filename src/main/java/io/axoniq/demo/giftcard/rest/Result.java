package io.axoniq.demo.giftcard.rest;

import org.axonframework.axonserver.connector.ErrorCode;

public record Result(
        boolean isSuccess,
        String error
) {

    public static Result ok() {
        return new Result(true, null);
    }

    public static Result Error(String id, String error) {
        if (error.contains(ErrorCode.INVALID_EVENT_SEQUENCE.errorCode())) {
            return new Result(false,
                              "An event for aggregate [" + id + "] at sequence ["
                                      + error.substring(error.length() - 1) + "] was already inserted. "
                                      + "You are either reusing the aggregate identifier "
                                      + "or concurrently dispatching commands for the same aggregate.");
        }
        return new Result(false, "Error on aggregate [" + id + "]. " + error);
    }
}
