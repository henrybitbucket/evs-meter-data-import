package com.pa.evs.sv.impl;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pa.evs.model.Log;
import com.pa.evs.repository.LogRepository;
import com.pa.evs.sv.LogService;

@Service
public class LogServiceImpl implements LogService {

    @Autowired
    LogRepository logRepository;

    @Override
    public List<Log> getRelatedLogs(Map<String, String> map) {
        String uid = map.get("uid");
        String msn = map.get("msn");
        String ptype = map.get("ptype");
        
        if (StringUtils.isNotBlank(ptype)) {
            return logRepository.getRelatedLogsWithPtype(uid, msn, ptype);
        }
        
        return logRepository.getRelatedLogs(uid, msn);
    }
}
