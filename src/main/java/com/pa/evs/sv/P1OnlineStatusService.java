package com.pa.evs.sv;

import com.pa.evs.dto.P1OnlineStatusDto;
import com.pa.evs.dto.PaginDto;

public interface P1OnlineStatusService {
	
	void search(PaginDto<P1OnlineStatusDto> pagin);
	
}
