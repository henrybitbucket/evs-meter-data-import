package com.pa.evs.enums;

public enum DeviceEnrollType {
	Normal,
	BLEMaster,
	BLESlave;

	public static DeviceEnrollType from(String type) {
		for (DeviceEnrollType t : DeviceEnrollType.values()) {
			if (t.toString().equalsIgnoreCase(type)) {
				return t;
			}
		}
		return DeviceEnrollType.Normal;
	}
}
