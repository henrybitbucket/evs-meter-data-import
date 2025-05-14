package com.pa.evs.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.Schema.RequiredMode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CreateAccessPermissionDto {

    @Schema(requiredMode = RequiredMode.REQUIRED)
    private String name;
    
    @Schema(description = "Start date format MM/dd/YYYY")
    private String startDate;
    
    @Schema(description = "End date format MM/dd/YYYY")
    private String endDate;
    
    private Boolean isAllWeek;
    
    private Integer startHour;
    private Integer startMinute;
    private Integer endHour;
    private Integer endMinute;
	
}
