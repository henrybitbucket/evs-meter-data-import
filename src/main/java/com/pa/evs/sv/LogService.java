package com.pa.evs.sv;

import com.pa.evs.dto.LogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.Log;

import java.text.ParseException;
import java.util.Map;

public interface LogService {

    PaginDto<Log> getRelatedLogs(PaginDto<Log> pagin) throws ParseException;
    
    void searchLog (PaginDto<LogDto> pagin);

	Object getMeterLog(Map<String, Object> map);

	String getKwh(String uid, Map<String, Object> map) throws Exception;

}
