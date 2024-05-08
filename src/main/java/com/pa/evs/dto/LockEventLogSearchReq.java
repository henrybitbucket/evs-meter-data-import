package com.pa.evs.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class LockEventLogSearchReq {
	
	@Schema(nullable = false)
	private CredentialDto credential;
	
	@Schema(nullable = false)
	@Builder.Default
	private RequestDto request = new RequestDto();
	
	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	@Builder
	public static class CredentialDto {
		@JsonProperty("username")
		@Schema(nullable = false)
		String username;
		
		@JsonProperty("user_id")
		Long userId;
	}
	
	@AllArgsConstructor
	@NoArgsConstructor
	@Data
	@Builder
	public static class RequestDto {
		@JsonProperty("bid")
		String bid;
		
		@JsonProperty("from_timestamp")
		Instant from;
		
		@JsonProperty("to_timestamp")
		Instant to;
	}
	
}
