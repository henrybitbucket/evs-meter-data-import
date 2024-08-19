package com.pa.evs.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pa.evs.model.DMSLockEventLog;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LockEnventLogResDto {
	
	@JsonProperty(value = "log_timestamp")
	private Instant time;

	private String bid;
	
	private String mobile;
	
	@Schema(description = "Operation type")
	private String typeCode;
	
	@Schema(description = "Operation result")
	private String resultCode;
	
	private String battery;
	
	@Schema(description = "long")
	@JsonProperty(value = "long")
	private String lng;
	
	@Schema(description = "lat")
	private String lat;
	
	private Boolean offlineMode;
	
	public static LockEnventLogResDto from(DMSLockEventLog fr) {
		return builder()
				.time(Instant.ofEpochMilli(fr.getCreateDate().getTime()))
				.bid(fr.getBid())
				.typeCode(fr.getTypeCode())
				.resultCode(fr.getResultCode())
				.battery(fr.getBattery())
				.lng(fr.getLng())
				.lat(fr.getLat())
				.mobile(fr.getCreatedBy())
				.offlineMode(fr.getOfflineMode())
				.build();
	}
}
