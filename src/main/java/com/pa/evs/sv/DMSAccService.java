package com.pa.evs.sv;

import com.pa.evs.dto.DMSAccDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.VendorDMSAccDto;
import com.pa.evs.dto.VendorDto;

public interface DMSAccService {

	void getDMSMCUsers(PaginDto<DMSAccDto> pagin);

	void saveDMSMCUser(DMSAccDto user);

	void saveOrUpdateVendorAndUser(VendorDMSAccDto dto);

	VendorDto getVendorAndMcAccs(Long vendorId);

	void deleteVendor(Long vendorId);

	void getVendorsUsers(PaginDto<VendorDto> pagin);

}
