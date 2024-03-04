package com.pa.evs.dto;

import com.pa.evs.model.Company;

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
public class CompanyDto {

	private Long id;
	private String name;
	private String description;
	
	public static CompanyDto build(Company cpn) {
		return builder()
				.id(cpn.getId())
				.name(cpn.getName())
				.description(cpn.getDescription())
				.build();
	}
	
}
