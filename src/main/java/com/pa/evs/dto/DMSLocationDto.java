package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DMSLocationDto {

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
	private String displayName;
	
	@Builder.Default
	private List<DMSLockDto> locks = new ArrayList<>();

}
