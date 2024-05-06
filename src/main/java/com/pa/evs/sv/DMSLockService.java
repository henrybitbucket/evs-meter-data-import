package com.pa.evs.sv;

import java.util.List;

import com.pa.evs.dto.DMSLocationLockDto;
import com.pa.evs.dto.DMSLockDto;
import com.pa.evs.dto.DMSLockVendorDto;
import com.pa.evs.dto.LockDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.SaveLogReq;

public interface DMSLockService {

	PaginDto<DMSLockDto> search(PaginDto<DMSLockDto> pagin);

	Object syncLock(Long vendorId);

	List<DMSLockVendorDto> getDMSLockVendors();

	void linkLocation(DMSLocationLockDto dto);

	void unLinkLocation(Long linkLockLocationId);

	Object getAssignedLocks(String email, Boolean lockOnly);
	
	Object getAssignedLocks2(String mobileNumber, Boolean lockOnly);

	Object getSecretCode(String email, Long dmsLockId);

	Object getSecretCode(String phone, LockDto lockDto);

	Object getSecretCode2(String userMobile, Long dmsLockId);

	void saveLog(SaveLogReq dto);
}
