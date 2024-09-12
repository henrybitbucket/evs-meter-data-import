package com.pa.evs.dto;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
@AllArgsConstructor
public class CredentialDto {

	String username;
	
	@JsonProperty("user_id")
	String userId;
}
