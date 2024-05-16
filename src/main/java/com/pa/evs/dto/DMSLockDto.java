package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
	
	@JsonProperty("bid")
    private String lockBid;
	
    private String lockEsn;
	
    private String lockName;
	
    @JsonProperty("lock_number")
    private String lockNumber;
	
    @JsonIgnore
    private boolean lastSyncExist;
	
	private DMSLockVendorDto vendor;
	
	private String homeAddress;
	
    private BuildingDto building;
    
    private BlockDto block;
    
    private FloorLevelDto floorLevel;
    
    private BuildingUnitDto buildingUnit;
    
    @JsonIgnore
    private Long linkLockLocationId;
    
	private Long siteId;
	
    private String siteLabel;
    
    @JsonProperty(value = "allowed_period")
    @Builder.Default
    private List<Map<String, Object>> allowedPeriod = new ArrayList<>();// site-id, list work-order, all application site
    
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
