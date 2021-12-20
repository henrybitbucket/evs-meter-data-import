package com.pa.evs.dto;

import java.util.Date;

import com.pa.evs.model.Building;


public class BuildingDto {
	private Long id;
	private String name;
	private String description;
	private Date createdDate;
	private Date modifiedDate;
	private String type;
	private AddressDto address;
	private Boolean hasTenant;
	
	private String label;
	
	public BuildingDto() {
		
	}
	
	public BuildingDto(Building fr) {
		
		if (fr != null) {
			this.id = fr.getId();
		}
	}

	public String getType() {
		return type;
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

	public void setType(String type) {
		this.type = type;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
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

	public AddressDto getAddress() {
		return address;
	}

	public void setAddress(AddressDto address) {
		this.address = address;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	
}
