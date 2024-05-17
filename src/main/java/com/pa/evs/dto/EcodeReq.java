package com.pa.evs.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class EcodeReq {
	
	@JsonProperty(value = "dotc")
    private String dotc;
	
	@JsonProperty(value = "mobile")
    private String mobile;
	
	@JsonProperty(value = "building")
    private String building;
	
	@JsonProperty(value = "level")
    private String level;
	
}
