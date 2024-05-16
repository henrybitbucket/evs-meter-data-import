package com.pa.evs.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LockWorkOrderReq {
	
	@JsonProperty(value = "from_timestamp")
    private Instant from;
	
	@JsonProperty(value = "to_timestamp")
    private Instant to;
	
}
