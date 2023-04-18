package com.pa.evs.dto;

import java.util.Date;
import java.util.List;

import com.pa.evs.model.BuildingUnit;

public class BuildingUnitDto {
	private Long id;
	private String name;
	private String displayName;
	private String description;
	private String type;
	private Boolean hasTenant;
	private FloorLevelDto floorLevel;

	private String unit;
	private Date createdDate;
	private Date modifiedDate;
	private List<String> names;
	private String remark;

	public BuildingUnitDto() {

	}

	public BuildingUnitDto(BuildingUnit fr) {
		if (fr != null) {
			this.id = fr.getId();
		}
	}

	public Date getCreatedDate() {
		return createdDate;
	}

	public void setCreatedDate(Date createdDate) {
		this.createdDate = createdDate;
	}

	public Date getModifiedDate() {
		return modifiedDate;
	}

	public void setModifiedDate(Date modifiedDate) {
		this.modifiedDate = modifiedDate;
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

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Boolean getHasTenant() {
		return hasTenant;
	}

	public void setHasTenant(Boolean hasTenant) {
		this.hasTenant = hasTenant;
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public FloorLevelDto getFloorLevel() {
		return floorLevel;
	}

	public void setFloorLevel(FloorLevelDto floorLevel) {
		this.floorLevel = floorLevel;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public List<String> getNames() {
		return names;
	}

	public void setNames(List<String> names) {
		this.names = names;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

}
