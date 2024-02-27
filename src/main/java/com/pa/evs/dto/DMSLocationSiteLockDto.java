package com.pa.evs.dto;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DMSLocationSiteLockDto {

	@Builder.Default
	List<DMSSiteDto> sites = new ArrayList<>();
	
	@Builder.Default
	List<DMSLockDto> locks = new ArrayList<>();

}
