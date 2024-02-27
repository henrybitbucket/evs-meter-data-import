package com.pa.evs.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.pa.evs.model.DMSLock;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DMSLockDto {

	private Long id;
	
    private String originalId;
	
    private String lockBid;
	
    private String lockEsn;
	
    private String lockName;
	
    private String lockNumber;
	
    @JsonIgnore
    private boolean lastSyncExist;
	
	private DMSLockVendorDto vendor;
	
	private String homeAddress;
	
    private BuildingDto building;
    
    private BlockDto block;
    
    private FloorLevelDto floorLevel;
    
    private BuildingUnitDto buildingUnit;
    
    private Long linkLockLocationId;
    
	@JsonIgnore
	private Long siteId;
	
    private String siteLabel;

	public static DMSLockDto build(DMSLock li) {
		
		return DMSLockDto
				.builder()
				.id(li.getId())
				.lockBid(li.getLockBid())
				.lockEsn(li.getLockEsn())
				.lockName(li.getLockName())
				.lockNumber(li.getLockNumber())
				.originalId(li.getOriginalId())
				.vendor(DMSLockVendorDto.build(li.getVendor()))
				.build();
	}
}
