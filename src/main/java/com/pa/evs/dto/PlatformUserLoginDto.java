package com.pa.evs.dto;

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
public class PlatformUserLoginDto {

    private Long id;

    private String name;// mobile/ web
	
    private String description;// android/ ios/ ....
	
	private Boolean active;
	
	private String email;
	
	private Long startTime;
	
	private Long endTime;
}
