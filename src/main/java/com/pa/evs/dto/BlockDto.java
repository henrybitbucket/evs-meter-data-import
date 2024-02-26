package com.pa.evs.dto;

import java.util.List;

import com.pa.evs.model.Block;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@AllArgsConstructor
@Getter
@Setter
@Builder
public class BlockDto {
	private Long id;
	private String name;
	private String displayName;
	private String block;
	private Boolean hasTenant;
	private BuildingDto building;
	private List<String> names;
	private List<FloorLevelDto> levels;

	public BlockDto() {
		
	}
	
	public BlockDto(Block fr) {
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

	public String getBlock() {
		return block;
	}
	public void setBlock(String block) {
		this.block = block;
	}
	public List<FloorLevelDto> getLevels() {
		return levels;
	}
	public void setLevels(List<FloorLevelDto> levels) {
		this.levels = levels;
	}

	public List<String> getNames() {
		return names;
	}

	public void setNames(List<String> names) {
		this.names = names;
	}
	
}
