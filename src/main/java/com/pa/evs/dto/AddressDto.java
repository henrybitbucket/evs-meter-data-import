package com.pa.evs.dto;

import java.util.Date;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pa.evs.model.Address;

/**
 * @author tonyk
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AddressDto {
	private Long id;
	private String country;
	private String city;
	private String town;
	private String street;
	private String postalCode;
	private String building;
	private String block;
	private String level;
	private Long levelId;
	private String unitNumber;
	private Long unitId;
	private String displayName;
	private Date createDate;
	private Date modifyDate;
	private String createdBy;
	private String updatedBy;
	private String remark;
	private String remarkForMeter;
	private String locationTag;
	private String coupleState;
	private String coupleMsn;
	private String importMsn;
	
	private boolean skipAddress;

	private String coupleSn;

	private String coupleUid;

	private String streetNumber;

	private Date coupleTime;

	private String message;

	private int line;

	private Map<String, Integer> head;

	public AddressDto() {

	}

	public AddressDto(Address fr) {
		if (fr != null) {
			this.id = fr.getId();
			this.streetNumber = fr.getStreetNumber();
			this.street = fr.getStreet();
			this.town = fr.getTown();
			this.city = fr.getCity();
			this.country = fr.getCountry();
			this.postalCode = fr.getPostalCode();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public Date getModifyDate() {
		return modifyDate;
	}

	public void setModifyDate(Date modifyDate) {
		this.modifyDate = modifyDate;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getCountry() {
		return country;
	}

	public void setCountry(String country) {
		this.country = country;
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
	}

	public String getTown() {
		return town;
	}

	public void setTown(String town) {
		this.town = town;
	}

	public String getStreet() {
		return street;
	}

	public void setStreet(String street) {
		this.street = street;
	}

	public String getPostalCode() {
		return postalCode;
	}

	public void setPostalCode(String postalCode) {
		this.postalCode = postalCode;
	}

	public String getBlock() {
		return block;
	}

	public void setBlock(String block) {
		this.block = block;
	}

	public String getUnitNumber() {
		return unitNumber;
	}

	public void setUnitNumber(String unitNumber) {
		this.unitNumber = unitNumber;
	}

	public String getStreetNumber() {
		return streetNumber;
	}

	public void setStreetNumber(String streetNumber) {
		this.streetNumber = streetNumber;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public String getRemark() {
		return remark;
	}

	public void setRemark(String remark) {
		this.remark = remark;
	}

	public String getBuilding() {
		return building;
	}

	public void setBuilding(String building) {
		this.building = building;
	}

	public String getCoupleState() {
		if (coupleState == null) {
			coupleState = "N";
		}
		return coupleState;
	}

	public void setCoupleState(String coupleState) {
		this.coupleState = coupleState;
	}

	public String getCoupleMsn() {
		return coupleMsn;
	}

	public void setCoupleMsn(String coupleMsn) {
		this.coupleMsn = coupleMsn;
	}

	public String getCoupleUid() {
		return coupleUid;
	}

	public void setCoupleUid(String coupleUid) {
		this.coupleUid = coupleUid;
	}

	public String getCoupleSn() {
		return coupleSn;
	}

	public void setCoupleSn(String coupleSn) {
		this.coupleSn = coupleSn;
	}

	public Date getCoupleTime() {
		return coupleTime;
	}

	public void setCoupleTime(Date coupleTime) {
		this.coupleTime = coupleTime;
	}

	public Long getUnitId() {
		return unitId;
	}

	public void setUnitId(Long unitId) {
		this.unitId = unitId;
	}

	public Long getLevelId() {
		return levelId;
	}

	public void setLevelId(Long levelId) {
		this.levelId = levelId;
	}

	public String getMessage() {
		return message;
	}

	public void setMessage(String message) {
		this.message = message;
	}

	public int getLine() {
		return line;
	}

	public void setLine(int line) {
		this.line = line;
	}

	public Map<String, Integer> getHead() {
		return head;
	}

	public void setHead(Map<String, Integer> head) {
		this.head = head;
	}

	public String getLocationTag() {
		return locationTag;
	}

	public void setLocationTag(String locationTag) {
		this.locationTag = locationTag;
	}

	public String getRemarkForMeter() {
		return remarkForMeter;
	}

	public void setRemarkForMeter(String remarkForMeter) {
		this.remarkForMeter = remarkForMeter;
	}

	public String getImportMsn() {
		return importMsn;
	}

	public void setImportMsn(String importMsn) {
		this.importMsn = importMsn;
	}

	public boolean isSkipAddress() {
		return skipAddress;
	}

	public void setSkipAddress(boolean skipAddress) {
		this.skipAddress = skipAddress;
	}
	
}
