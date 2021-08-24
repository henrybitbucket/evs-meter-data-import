package com.pa.evs.sv;

import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.CARequestLog;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CaRequestLogService {
    
	Optional<CARequestLog> findByUid(String uid);
	
	void save(CaRequestLogDto dto);

	void linkMsn(Map<String, Object> map);
	
    PaginDto<CARequestLog> search(PaginDto<CARequestLog> pagin);

    File downloadCsv(List<CARequestLog> listInput) throws IOException;

    List<String> getCids();
}
