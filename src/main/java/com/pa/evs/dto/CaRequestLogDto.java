package com.pa.evs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Getter
@Setter
public class CaRequestLogDto {
    
    private Long id;

    private String uid;
    
    private String msn;
    
    private String certificate;
    
    private String raw;
    
    private Long startDate;
    
    private Long endDate;
    
    private Boolean requireRefresh;
    
    private Integer installer;
    
    private Integer group;
    
	private String homeAddress;

    private Long buildingId;
    
    private Long floorLevelId;
    
    private Long buildingUnitId;
    
    private BuildingDto building;
    
    private FloorLevelDto floorLevel;
    
    private BuildingUnitDto buildingUnit;
    
    private AddressDto address;
}
