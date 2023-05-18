package com.pa.evs.dto;

import com.pa.evs.model.Vendor;

public class VendorDto {

	private Long id;
	private String name;
	private String descrption;
	
	public VendorDto() {}
	
	public VendorDto(Vendor vendor) {
		this.id = vendor.getId();
		this.name = vendor.getName();
		this.descrption = vendor.getDescription();
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

	public String getDescrption() {
		return descrption;
	}

	public void setDescrption(String descrption) {
		this.descrption = descrption;
	}
}
