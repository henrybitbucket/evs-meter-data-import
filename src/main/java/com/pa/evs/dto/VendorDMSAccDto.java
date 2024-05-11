package com.pa.evs.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class VendorDMSAccDto {

	private VendorDto vendor;
	private List<DMSAccDto> dmsAccDtos;

}
