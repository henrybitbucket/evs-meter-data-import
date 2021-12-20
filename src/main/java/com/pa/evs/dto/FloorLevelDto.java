package com.pa.evs.dto;

import com.pa.evs.model.FloorLevel;

public class FloorLevelDto {
	private Long id;
	private String name;
	private String displayName;
	private String level;
	private Boolean hasTenant;
	private BuildingDto building;
	
	public FloorLevelDto() {
		
	}
	
	public FloorLevelDto(FloorLevel fr) {
		if (fr != null) {
			this.id = fr.getId();
		}
	}
	
	public Long getId() {
		return id;
	}
	public void setId(Long id) {
		this.id = id;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public Boolean getHasTenant() {
		return hasTenant;
	}
	public void setHasTenant(Boolean hasTenant) {
		this.hasTenant = hasTenant;
	}
	public BuildingDto getBuilding() {
		return building;
	}
	public void setBuilding(BuildingDto building) {
		this.building = building;
	}

	public String getLevel() {
		return level;
	}
	public void setLevel(String level) {
		this.level = level;
	}
}
