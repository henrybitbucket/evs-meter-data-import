package com.pa.evs.dto;

import java.util.List;

import com.pa.evs.model.FloorLevel;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class FloorLevelDto {
	private Long id;
	private String name;
	private String displayName;
	private String level;
	private Boolean hasTenant;
	private BuildingDto building;
	private BlockDto block;
	private List<BuildingUnitDto> units;
	private List<String> names;
	
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

	public BlockDto getBlock() {
		return block;
	}

	public void setBlock(BlockDto block) {
		this.block = block;
	}

	public List<BuildingUnitDto> getUnits() {
		return units;
	}

	public void setUnits(List<BuildingUnitDto> units) {
		this.units = units;
	}

	public List<String> getNames() {
		return names;
	}

	public void setNames(List<String> names) {
		this.names = names;
	}
	
}
