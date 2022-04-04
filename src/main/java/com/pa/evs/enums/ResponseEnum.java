package com.pa.evs.enums;

public enum ResponseEnum {
    SUCCESS(0, "Successful operation."),
    TASK_IS_NOT_EXISTS(104, "Task is not exist"),
    SYSTEM_ERROR(99, "Provisioning portal system error"),
    BAD_REQUEST(999, "Bad request"),
    
    BUILDING_NOT_FOUND(100, "Building is not exist"),
    BUILDING_UNIT_NOT_FOUND(100, "Building Unit is not exist"),
    FLOOR_LEVEL_NOT_FOUND(100, "Floor Level is not exist"),
    BLOCK_NOT_FOUND(100, "Block is not exist"),
    
    ROLE_EXIST(999, "Role exist"),

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
