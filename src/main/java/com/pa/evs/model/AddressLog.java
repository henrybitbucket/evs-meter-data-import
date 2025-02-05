package com.pa.evs.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

import com.pa.evs.enums.DeviceType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Entity
@Table(name = "address_log")
public class AddressLog extends BaseEntity {

	@Column(name = "sn")
	private String sn;

	@Column(name = "msn")
	private String msn;

	@Column(name = "country")
	private String country;

	@Column(name = "city")
	private String city;

	@Column(name = "town")
	private String town;

	@Column(name = "street")
	private String street;

	@Column(name = "street_number")
	private String streetNumber;

	@Column(name = "postal_code")
	private String postalCode;

	@Column(name = "building")
	private String building;

	@Column(name = "block")
	private String block;

	@Column(name = "level")
	private String level;

	@Column(name = "unit_number")
	private String unitNumber;

	@Column(name = "remark")
	private String remark;

	@Column(name = "d_type")
	@Enumerated(EnumType.STRING)
	private DeviceType type;

	@Column(name = "building_id")
	private Long buildingId;

	@Column(name = "block_id")
	private Long blockId;

	@Column(name = "level_id")
	private Long levelId;

	@Column(name = "unit_id")
	private Long unitId;

	public static AddressLog build(CARequestLog ca) {
		boolean isAddress = ca.getBuilding() != null && ca.getBuilding().getAddress() != null;

		return builder()
				.msn(ca.getMsn())
				.sn(ca.getSn())
				.building(ca.getBuilding() != null ? ca.getBuilding().getName() : null)
				.block(ca.getBlock() != null ? ca.getBlock().getName() : null)
				.level(ca.getFloorLevel() != null ? ca.getFloorLevel().getName() : null)
				.unitNumber(ca.getBuildingUnit() != null ? ca.getBuildingUnit().getName() : null)
				.city(isAddress ? ca.getBuilding().getAddress().getCity() : null)
				.country(isAddress ? ca.getBuilding().getAddress().getCountry() : null)
				.street(isAddress ? ca.getBuilding().getAddress().getStreet() : null)
				.streetNumber(isAddress ? ca.getBuilding().getAddress().getStreetNumber() : null)
				.remark(isAddress ? ca.getBuilding().getAddress().getRemark() : null)
				.postalCode(isAddress ? ca.getBuilding().getAddress().getPostalCode() : null)
				.buildingId(ca.getBuilding() != null ? ca.getBuilding().getId() : null)
				.blockId(ca.getBlock() != null ? ca.getBlock().getId() : null)
				.levelId(ca.getFloorLevel() != null ? ca.getFloorLevel().getId() : null)
				.unitId(ca.getBuildingUnit() != null ? ca.getBuildingUnit().getId() : null)
				.build();
	}
	
	public static AddressLog buildMeterAddress(MMSMeter ca) {
		boolean isAddress = ca.getBuilding() != null && ca.getBuilding().getAddress() != null;

		return builder()
				.msn(ca.getMsn())
				.building(ca.getBuilding() != null ? ca.getBuilding().getName() : null)
				.block(ca.getBlock() != null ? ca.getBlock().getName() : null)
				.level(ca.getFloorLevel() != null ? ca.getFloorLevel().getName() : null)
				.unitNumber(ca.getBuildingUnit() != null ? ca.getBuildingUnit().getName() : null)
				.city(isAddress ? ca.getBuilding().getAddress().getCity() : null)
				.country(isAddress ? ca.getBuilding().getAddress().getCountry() : null)
				.street(isAddress ? ca.getBuilding().getAddress().getStreet() : null)
				.streetNumber(isAddress ? ca.getBuilding().getAddress().getStreetNumber() : null)
				.remark(isAddress ? ca.getBuilding().getAddress().getRemark() : null)
				.postalCode(isAddress ? ca.getBuilding().getAddress().getPostalCode() : null)
				.buildingId(ca.getBuilding() != null ? ca.getBuilding().getId() : null)
				.blockId(ca.getBlock() != null ? ca.getBlock().getId() : null)
				.levelId(ca.getFloorLevel() != null ? ca.getFloorLevel().getId() : null)
				.unitId(ca.getBuildingUnit() != null ? ca.getBuildingUnit().getId() : null)
				.build();
	}
}
