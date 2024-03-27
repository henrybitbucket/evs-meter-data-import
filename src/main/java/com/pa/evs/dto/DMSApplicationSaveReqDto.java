package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
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
public class DMSApplicationSaveReqDto {

	private Long projectId;
	
	@Schema(hidden = true)
	@JsonIgnore
	private String submittedBy;
	
	@Builder.Default
	private List<DMSApplicationSiteItemReqDto> sites = new ArrayList<>();
	
	@Builder.Default
	private List<String> userPhones = new ArrayList<>();
	
	@Builder.Default
	private List<DMSApplicationUserGuestReqDto> guests = new ArrayList<>();
	
	
	// time period all sites
	@Builder.Default
	private DMSTimePeriodReqDto timePeriod = new DMSTimePeriodReqDto();
}
