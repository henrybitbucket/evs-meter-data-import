package com.pa.evs.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class MenuItemsDto {

	private Long id;
	private String appCode;
    private String items;
	private String permissions;
	private String roles;
	private String groups;
    
}
