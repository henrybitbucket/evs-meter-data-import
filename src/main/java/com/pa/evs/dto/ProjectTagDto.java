package com.pa.evs.dto;

import com.pa.evs.model.ProjectTag;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@Getter
@Setter
public class ProjectTagDto {

	private Long id;
	private String name;
	private String description;
	
	public static ProjectTagDto build(ProjectTag tag) {
		return builder()
				.id(tag.getId())
				.name(tag.getName())
				.description(tag.getDescription())
				.build();
	}
	
}
