package com.pa.evs.sv;

import com.pa.evs.model.Log;

import java.util.List;
import java.util.Map;

public interface LogService {

    List<Log> getRelatedLogs(Map<String, Object> map);

	Object getMeterLog(Map<String, Object> map);

	String getKwh(String uid, Map<String, Object> map) throws Exception;

}
