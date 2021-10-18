package com.pa.evs.sv;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.CARequestLog;

public interface CaRequestLogService {
    
	Optional<CARequestLog> findByUid(String uid);
	
	void save(CaRequestLogDto dto) throws Exception;

	void linkMsn(Map<String, Object> map);
	
    PaginDto<CARequestLog> search(PaginDto<CARequestLog> pagin);

    File downloadCsv(List<CARequestLog> listInput, Long activateDate) throws IOException;

    List<String> getCids(boolean refresh);

    void setActivationDate(Long activationDate, Set<Long> ids);
}
