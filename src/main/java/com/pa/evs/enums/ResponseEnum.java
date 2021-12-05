package com.pa.evs.enums;

public enum ResponseEnum {
    SUCCESS(0, "Successful operation."),
    TASK_IS_NOT_EXISTS(104, "Task is not exist"),
    SYSTEM_ERROR(99, "Provisioning portal system error"),
    BAD_REQUEST(999, "Bad request"),
    ;

    private final int errorCode;
    private final String errorDescription;

    ResponseEnum (Integer errorCode, String errorDescription) {
        this.errorCode = errorCode;
        this.errorDescription = errorDescription;
    }

    public int getErrorCode() {
        return errorCode;
    }

    public String getErrorDescription() {
        return errorDescription;
    }
}
