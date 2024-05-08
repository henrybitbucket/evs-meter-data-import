package com.pa.evs.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.pa.evs.dto.SaveLogReq;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "dms_lock_event_log")
@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})
public class DMSLockEventLog extends BaseEntity {
	
	@Column(name = "lock_Number")
    private String lockNumber;
	
	@Column(name = "lock_bid")
    private String bid;
	
	@Column(name = "type_code")
    private String typeCode;
	
	@Column(name = "result_code")
    private String resultCode;
	
	@Column(name = "battery")
    private String battery;
	
	@Column(name = "long")
    private String lng;
	
	@Column(name = "lat")
    private String lat;
	
	public static DMSLockEventLog from(SaveLogReq fr) {
		return builder()
				.lockNumber(fr.getLockNumber())
				.bid(fr.getBid())
				.typeCode(fr.getTypeCode())
				.resultCode(fr.getResultCode())
				.battery(fr.getBattery())
				.lng(fr.getLng())
				.lat(fr.getLat())
				.build();
	}
}
