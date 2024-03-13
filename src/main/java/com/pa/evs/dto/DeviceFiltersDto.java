package com.pa.evs.dto;

import com.pa.evs.model.Users;

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
public class DeviceFiltersDto {
	
	private Long id;
	private String name;
	private String filters;
	private Users user;
}
