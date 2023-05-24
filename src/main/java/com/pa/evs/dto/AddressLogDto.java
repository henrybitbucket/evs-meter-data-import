package com.pa.evs.dto;

import java.util.Date;

import com.pa.evs.enums.DeviceType;

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

}
