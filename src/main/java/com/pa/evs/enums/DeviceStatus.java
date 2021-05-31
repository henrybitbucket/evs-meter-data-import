package com.pa.evs.enums;

public enum DeviceStatus {
    ONLINE("Online"),
    OFFLINE("Offline");

    private String status;

    DeviceStatus(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
