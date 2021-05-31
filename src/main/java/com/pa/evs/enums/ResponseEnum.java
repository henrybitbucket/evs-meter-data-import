package com.pa.evs.enums;

public enum ResponseEnum {
    SUCCESS(0, "Successful operation."),
    GATEWAY_NOT_FOUND(100, "Gateway is not exist"),
    DEVICE_IS_EXISTS(101, "Device is exist. It should be removed before adding again"),
    DEVICE_IS_NOT_EXISTS(102, "Device is not exist"),
    INVALID_DEVICE_TYPE(103, "Invalid device type"),
    BUILDING_NOT_FOUND(100, "Building is not exist"),
    BUILDING_UNIT_NOT_FOUND(100, "Building Unit is not exist"),
    FLOOR_LEVEL_NOT_FOUND(100, "Floor Level is not exist"),


    //error code
    RM_API_ERROR(600, "Error when calling RM API"),
    FIM_ERROR(700, "Functional item error"),
    SWIFT_SENSE_ERROR(800, "Error when calling SwiftSense API"),
    SWIFT_SENSE_GET_TOKEN_ERROR(801, "Error when get access token by SwiftSense API"),
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
