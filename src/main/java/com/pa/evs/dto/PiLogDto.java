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
public class PiLogDto {

	private String type;
	
	private String msn;
	
	private Long mid;
	
	private String ftpResStatus;
	
	private String piUuid;
}
