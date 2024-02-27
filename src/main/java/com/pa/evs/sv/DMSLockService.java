package com.pa.evs.sv;

import java.util.List;

import com.pa.evs.dto.DMSLocationLockDto;
import com.pa.evs.dto.DMSLockDto;
import com.pa.evs.dto.DMSLockVendorDto;
import com.pa.evs.dto.PaginDto;

public interface DMSLockService {

	PaginDto<DMSLockDto> search(PaginDto<DMSLockDto> pagin);

	Object syncLock(Long vendorId);

	List<DMSLockVendorDto> getDMSLockVendors();

	void linkLocation(DMSLocationLockDto dto);

	void unLinkLocation(Long linkLockLocationId);

	Object getAssignedLocks(String email, Boolean lockOnly);
}
