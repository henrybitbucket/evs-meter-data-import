package com.pa.evs.sv;

import java.util.List;
import java.util.Map;

import com.pa.evs.model.Log;

public interface LogService {

    List<Log> getRelatedLogs(Map<String, String> map);
}
