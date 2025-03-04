package com.pa.evs.dto;

import java.util.List;

import com.pa.evs.enums.VendorType;
import com.pa.evs.model.Vendor;

import lombok.Builder;

/**
 * @author tonyk
 *
 */
public class VendorDto {

	private Long id;
	private String name;
	private String descrption;
	private String signatureAlgorithm;
	private String keyType;
	
    private Long maxMidValue;
    
    private Integer midResetTime;

	public VendorDto() {
	}

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

	public String getSignatureAlgorithm() {
		return signatureAlgorithm;
	}

	public void setSignatureAlgorithm(String signatureAlgorithm) {
		this.signatureAlgorithm = signatureAlgorithm;
	}

	public String getKeyType() {
		return keyType;
	}

	public void setKeyType(String keyType) {
		this.keyType = keyType;
	}

	public Long getMaxMidValue() {
		return maxMidValue;
	}

	public void setMaxMidValue(Long maxMidValue) {
		this.maxMidValue = maxMidValue;
	}

	public Integer getMidResetTime() {
		return midResetTime;
	}

	public void setMidResetTime(Integer midResetTime) {
		this.midResetTime = midResetTime;
	}
	
}
