package com.pa.evs.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationRequestDto {

	CredentialDto credential;
	ApplicationRequestDetailDto request;

	@Builder
	@Getter
	@Setter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class ApplicationRequestDetailDto {

		@JsonInclude(JsonInclude.Include.NON_NULL)
		@JsonProperty(value = "from_timestamp")
	    private Instant from;
		
		@JsonInclude(JsonInclude.Include.NON_NULL)
		@JsonProperty(value = "to_timestamp")
	    private Instant to;
	}

}