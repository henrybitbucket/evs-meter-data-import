package com.pa.evs.enums;

import java.util.Arrays;

public enum MqttCmdStatus {
	
//    @JsonIgnore
//    public String getRepStatusDesc() {
//		if (repStatus == null) return "NO RESPONSE";
//    	return repStatus == 0 ? "OK"
//                : repStatus == 1 ? "Invalid Format"
//                : repStatus == 2 ? "Invalid Command"
//                : repStatus == 3 ? "Invalid Signature"
//                : repStatus == 4 ? "Decryption Failed"
//                : repStatus == 5 ? "Invalid Configuration"
//                : repStatus == 8 ? "Failed send to device (Gateway)"
//                : repStatus == 9 ? "General Error"
//                : "NO RESPONSE";
//    }
	OK(0, "OK"), 
	INVALID_FORMAT(1, "Invalid Format"),
	INVALID_COMMAND(2, "Invalid Command"), 
	INVALID_SIGNATURE(3, "Invalid Signature"),

	DECRYPTION_FAILED(4, "Decryption Failed"), 
	INVALID_CONFIGURATION(5, "Invalid Configuration"),
	FAILED_SEND_TO_DEVICE(8, "Failed send to device (Gateway)"), 
	GENERAL_ERROR(9, "General Error"),
	INVALID_DEVICE(10, "invalid device"),
	;

	private final int status;
	private final String description;

	MqttCmdStatus(Integer status, String description) {
		this.status = status;
		this.description = description;
	}

	public int getStatus() {
		return status;
	}

	public String getDescription() {
		return description;
	}

	public static String getDescription(final Number stt) {
		try {
			if (stt == null) {
				return "NO RESPONSE";
			}
			
			MqttCmdStatus cmdStatus = Arrays.stream(MqttCmdStatus.values()).filter(mqStt -> mqStt.getStatus() == stt.intValue()).findFirst().orElse(null);
			if (cmdStatus != null) {
				return cmdStatus.getDescription();
			}
			
			return "NO RESPONSE";
		} catch (Exception e) {
			return "NO RESPONSE";
		}
		
	}
}
