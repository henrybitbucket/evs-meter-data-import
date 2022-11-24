package com.pa.evs.sv;

import java.util.List;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.dto.SettingDto;

public interface SettingService {
	
	String TIME_CHECK_PI_ONLINE  = "TIME_CHECK_PI_ONLINE";
	
	String TIME_LOGIN_EXPIRED  = "TIME_LOGIN_EXPIRED";
	
	List<SettingDto> findAll();
	
	SettingDto findByKey(String key);
	
	void save(SettingDto dto);

	void delete(Long id);

	Object search(PaginDto<SettingDto> pagin);
}
