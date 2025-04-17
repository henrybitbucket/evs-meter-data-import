package com.pa.evs.dto;

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
public class DMSLocationLockDto {

	private Long lockId;
	
	private String lockName;

	private Long buildingId;
	
	private Long blockId;
	
	private Long floorLevelId;

	private Long buildingUnitId;

}
