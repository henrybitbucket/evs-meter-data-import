package com.pa.evs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
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
public class DMSWorkOrdersDto {

	private Long id;
	
	private String name;
	
	//private GroupUserDto group;

    private DMSSiteDto site;
    
    private DMSApplicationDto app;
    
    private Long siteId;
    
    private String siteLabel;
    
    @JsonProperty
    private Long applicationId;
	
	private String applicationEmail;
	
	@Column(name = "status")
	private String status;

	// timePeriod dates in year
	@Builder.Default
	private boolean timePeriodDatesIsAlways = false;
	private Long timePeriodDatesStart;
	private Long timePeriodDatesEnd;
	// end timePeriod dates in year
	
	// timePeriod days in week
	@Builder.Default
	private boolean timePeriodDayInWeeksIsAlways = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsMon = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsTue = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsWed = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsThu = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsFri = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsSat = false;
	@Builder.Default
	private boolean timePeriodDayInWeeksIsSun = false;
	// end timePeriod days in week
	
	// timePeriod time in day
	@Builder.Default
	private boolean timePeriodTimeInDayIsAlways = false;
	private Integer timePeriodTimeInDayHourStart;
	private Integer timePeriodTimeInDayHourEnd;
	private Integer timePeriodTimeInDayMinuteStart;
	private Integer timePeriodTimeInDayMinuteEnd;
}
