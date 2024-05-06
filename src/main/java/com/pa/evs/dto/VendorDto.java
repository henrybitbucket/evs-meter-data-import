package com.pa.evs.dto;

import java.util.List;

import com.pa.evs.enums.VendorType;
import com.pa.evs.model.Vendor;

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
	private VendorType type;
	private List<DMSAccDto> mcAccs;

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

	public VendorType getType() {
		return type;
	}

	public void setType(VendorType type) {
		this.type = type;
	}
	
	public List<DMSAccDto> getMcAccs() {
		return mcAccs;
	}

	public void setMcAccs(List<DMSAccDto> mcAccs) {
		this.mcAccs = mcAccs;
	}

}
