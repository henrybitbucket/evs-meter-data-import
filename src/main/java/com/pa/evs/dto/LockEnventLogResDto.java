package com.pa.evs.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pa.evs.model.DMSLock;
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

	private Long id;
	
	@JsonProperty(value = "lock_id")
	private String lockId;
	
	@JsonProperty(value = "log_timestamp")
	private Instant time;

	private String bid;
	
	@JsonProperty(value = "lock_name")
	private String lockName;
	
	@JsonProperty(value = "lock_number")
	private String lockNumber;
	
	@JsonProperty(value = "user_name")
	private String username;
	
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
	
	@JsonProperty(value = "location_name")
    private String locationName;
	
	@JsonInclude(JsonInclude.Include.NON_NULL)
	@JsonProperty(value = "session")
    private String session;
	
	public static LockEnventLogResDto from(DMSLockEventLog fr) {
		return from(fr, null);
	}
	
	public static LockEnventLogResDto from(DMSLockEventLog fr, DMSLock lock) {
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
				.lockName(lock == null ? null : lock.getLockName())
				.lockNumber(lock == null ? null : lock.getLockNumber())
				.lockId(lock == null ? null : lock.getOriginalId())
				.locationName(fr.getLocationName())
				.id(fr.getId())
				.build();
	}
}
