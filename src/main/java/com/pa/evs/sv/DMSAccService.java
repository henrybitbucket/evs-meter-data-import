package com.pa.evs.sv;

import java.util.List;

import com.pa.evs.dto.DMSAccDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.VendorDto;

public interface DMSAccService {

	void getDMSMCUsers(PaginDto<DMSAccDto> pagin);

	void saveDMSMCUser(DMSAccDto user);

	void saveVendorAndUser(VendorDto vendorDto, List<DMSAccDto> dmsAccDtos);

	VendorDto getVendorAndMcAccs(Long vendorId);

}
