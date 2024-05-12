package com.pa.evs.sv;

import com.pa.evs.dto.DMSAccDto;
import com.pa.evs.dto.DMSLockVendorDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.VendorDMSAccDto;

public interface DMSAccService {

	void getDMSMCUsers(PaginDto<DMSAccDto> pagin);

	void saveDMSMCUser(DMSAccDto user);

	void saveOrUpdateVendorAndUser(VendorDMSAccDto dto);

	DMSLockVendorDto getVendorAndMcAccs(Long vendorId);

	void deleteVendor(Long vendorId);

	void getVendorsUsers(PaginDto<DMSLockVendorDto> pagin);

}
