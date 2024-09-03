package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;


@AllArgsConstructor
@Getter
@Setter
public class DMSApplicationGuestSaveReqDto extends DMSApplicationSaveReqDto {

//	@Schema(hidden = true)
	private Long projectId;
	
	@Schema(hidden = true)
	private Long updatedDate;// for show history
	
	@Schema(hidden = true)
	private String updatedBy;// phone // for show history
	
	@Schema(hidden = true)
	@JsonIgnore
	private String submittedBy;
	
	@Builder.Default
	private List<DMSApplicationSiteItemReqDto> sites = new ArrayList<>();
	
	// time period all sites
	@Builder.Default
	private DMSTimePeriodDto timePeriod = new DMSTimePeriodDto();
	
	private Long timeTerminate;
	
	@Schema(hidden = true)
	private String otp;
}
