package com.pa.evs.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

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
@JsonIgnoreProperties(ignoreUnknown=true)
public class LocksAccessPermisisonDto {
	
    @Schema(requiredMode = RequiredMode.REQUIRED)
    private String fullName;
    
    @Schema(requiredMode = RequiredMode.REQUIRED)
    private String phoneNumber;

    @Schema(requiredMode = RequiredMode.REQUIRED)
    private String groupName;
    
    @Schema(description = "Start date format MM/dd/YYYY", requiredMode = RequiredMode.REQUIRED)
    private String startDate;
    
    @Schema(description = "End date format MM/dd/YYYY", requiredMode = RequiredMode.REQUIRED)
    private String endDate;
    
    @Schema(requiredMode = RequiredMode.REQUIRED)
    private Integer startHour;
    
    @Schema(requiredMode = RequiredMode.REQUIRED)
    private Integer startMinute;
    
    @Schema(requiredMode = RequiredMode.REQUIRED)
    private Integer endHour;
    
    @Schema(requiredMode = RequiredMode.REQUIRED)
    private Integer endMinute;
    
    private String email;
    	
    private String remarks;
    
    @Schema(hidden = true)
    private String url;
    
    @Schema(hidden = true)
    private String messageId;
    
}
