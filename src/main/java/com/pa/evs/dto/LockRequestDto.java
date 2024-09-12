package com.pa.evs.dto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import io.swagger.annotations.ApiModelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
public class LockRequestDto {

	CredentialDto credential;
	LockRequestDetailDto request;
	
	@Builder
	@Getter
	@Setter
	@AllArgsConstructor
	@NoArgsConstructor
	public static class LockRequestDetailDto {

		@ApiModelProperty(example = "", required = false)
		@Schema(description = "lock name", example = "", required = false)
		@JsonInclude(JsonInclude.Include.NON_NULL)
		@JsonProperty("lock_name")
		String lockName;

		@ApiModelProperty(example = "", required = false)
		@Schema(description = "lock bid", example = "", required = false)
		@JsonInclude(JsonInclude.Include.NON_NULL)
		@JsonProperty("lock_bid")
		String lockBid;
		
		@ApiModelProperty(example = "", required = false)
		@Schema(description = "lock number", example = "", required = false)
		@JsonInclude(JsonInclude.Include.NON_NULL)
		@JsonProperty("lock_number")
		String lockNumber;
	}

}
