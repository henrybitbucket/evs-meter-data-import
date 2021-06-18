package com.pa.evs.sv;

import java.util.Optional;

import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.CARequestLog;

public interface CaRequestLogService {
    
	Optional<CARequestLog> findByUid(String uid);
	
	void save(CaRequestLogDto dto);

    void search(PaginDto<CaRequestLogDto> pagin);
}
