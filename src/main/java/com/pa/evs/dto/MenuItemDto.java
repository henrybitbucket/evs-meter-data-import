package com.pa.evs.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@Data
public class MenuItemDto {
	private String id;
	private String className;
	private String name;
	private String path;
	private List<String> permissions;
	private List<String> roles;
	private List<String> groups;
	private List<MenuItemDto> children;
}
