package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;

import io.swagger.annotations.ApiModelProperty;
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

	@Schema(hidden = true)
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
	
	@Builder.Default
	private List<String> userPhones = new ArrayList<>();
	
	@Builder.Default
	private List<DMSApplicationUserGuestReqDto> guests = new ArrayList<>();
	
	// time period all sites
	@Builder.Default
	private DMSTimePeriodDto timePeriod = new DMSTimePeriodDto();
	
	private Long timeTerminate;
	
	@Schema(hidden = true)
	@Builder.Default
	@JsonIgnore
	private boolean isGuestSubmit = false;
	
	@Schema(hidden = true)
	@ApiModelProperty(hidden = true)
	@JsonIgnore
	private String tokenId;

	@Schema(hidden = true)
	@ApiModelProperty(hidden = true)
	@JsonIgnore
	private Long tokenStartTime;
	
	@Schema(hidden = true)
	@ApiModelProperty(hidden = true)
	@JsonIgnore
	private Long tokenEndTime;
}
