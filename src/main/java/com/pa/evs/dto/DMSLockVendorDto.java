package com.pa.evs.dto;

import java.util.List;

import com.pa.evs.enums.VendorType;
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
    private String label;
    private String companyName;
	private VendorType type;
	private List<DMSAccDto> mcAccs;
	
	public static DMSLockVendorDto build(DMSLockVendor vendor) {
		return DMSLockVendorDto
				.builder()
				.id(vendor.getId())
				.name(vendor.getName())
				.type(vendor.getType())
				.label(vendor.getLabel())
				.companyName(vendor.getCompanyName())
				.build();
	}
	
}
