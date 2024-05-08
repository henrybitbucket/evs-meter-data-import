package com.pa.evs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class SaveLogReq {

	@Schema(description = "Lock number", example = "100010010")
	private String lockNumber;
	
	private String bid;
	
	@Schema(description = "Operation type")
	private String typeCode;
	
	@Schema(description = "Operation result")
	private String resultCode;
	
	private String battery;
	
	@JsonProperty(value = "long")
	@Schema(description = "long (optional)")
	private String lng;
	
	@Schema(description = "lat (optional)")
	private String lat;
	
//
//	@Schema(description = "Lock type, 0-passive lock, 1-Bluetooth lock", example = "0", required = true)
//	private String lockType;
//	
//	@Schema(description = "Can be empty when Bluetooth lock", example = "", required = true)
//	private String esn;
//	
//	@Schema(description = "MAC address of Bluetooth lock", example = "D8:41:5D:A3:C0:2A", required = true)
//	private String bid;
//	
//	@Schema(description = "Operation type 0-Bluetooth unlocking 3-Bluetooth locking", example = "0", required = true)
//	private String type;
//	
//	@Schema(description = "Operation result 5-success", example = "5", required = true)
//	private String result;
//	
//	@Schema(description = "Operating time", example = "2023-10-27 16:51:00", required = true)
//	private String time;
//	
//	@Schema(description = "Longitude when unlocking", example = "116.522897", required = true)
//	private String lng;
//	
//	@Schema(description = "Latitude when unlocking", example = "39.911895", required = true)
//	private String lat;
//	
//	@Schema(description = "The key number for passive unlocking. It can be empty for Bluetooth unlocking.", example = "", required = true)
//	private String keyNumber;
//	
//    @Schema(description = "User mobile", example = "+6500001111", required = true)
//    @JsonProperty(value = "user_mobile")
//    private String userMobile;
}
