package com.pa.evs.dto;

import java.util.Date;

import com.pa.evs.enums.DeviceType;
import com.pa.evs.model.AddressLog;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Getter
@Setter
public class AddressLogDto {

	private Long id;
	private Date createdDate;
	private String sn;
	private String msn;
	private String country;
	private String city;
	private String town;
	private String street;
	private String streetNumber;
	private String postalCode;
	private String building;
	private String block;
	private String level;
	private String unitNumber;
	private String remark;
	private DeviceType type;

	public static AddressLogDto build(AddressLog addr) {
		return builder()
				.id(addr.getId())
				.block(addr.getBlock())
				.building(addr.getBuilding())
				.city(addr.getCity())
				.country(addr.getCountry())
				.level(addr.getLevel())
				.msn(addr.getMsn())
				.postalCode(addr.getPostalCode())
				.remark(addr.getRemark())
				.unitNumber(addr.getUnitNumber())
				.type(addr.getType())
				.town(addr.getTown())
				.streetNumber(addr.getStreetNumber())
				.street(addr.getStreet())
				.sn(addr.getSn())
				.createdDate(addr.getCreateDate())
				.build();
	}
}
