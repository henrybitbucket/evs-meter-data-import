package com.pa.evs.dto;

import com.pa.evs.model.DMSLockVendor;

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
public class DMSLockVendorDto {

	private Long id;
	private String name;
	
	public static DMSLockVendorDto build(DMSLockVendor vendor) {
		return DMSLockVendorDto
				.builder()
				.id(vendor.getId())
				.name(vendor.getName())
				.build();
	}
	
}
