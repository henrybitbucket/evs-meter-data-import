package com.pa.evs.sv;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;

import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.CARequestLog;

public interface CaRequestLogService {
    
	Optional<CARequestLog> findByUid(String uid);
	
	void save(CaRequestLogDto dto);

	void linkMsn(Map<String, Object> map);
	
    PaginDto<CARequestLog> search(PaginDto<CARequestLog> pagin);

    File downloadCsv(List<CARequestLog> listInput) throws IOException;
}
