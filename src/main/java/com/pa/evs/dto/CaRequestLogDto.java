package com.pa.evs.dto;

import javax.persistence.Column;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.pa.evs.model.Address;
import com.pa.evs.model.Building;
import com.pa.evs.model.BuildingUnit;
import com.pa.evs.model.FloorLevel;

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

	private Building building;
	
	private FloorLevel floorLevel;

	private BuildingUnit buildingUnit;

	private Address address;
}
