package com.pa.evs.sv.impl;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.Query;

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

    @Autowired
    EntityManager em;
    
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
    
    @Override
	public Object getMeterLog(Map<String, Object> map) {
    	String uid = (String) map.get("uid"); 
    	Long from = (Long) map.get("from");
    	Long to = (Long) map.get("to");
		
		if (from == null) {
			from = System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000l;
		}
		
		if (to == null) {
			to = System.currentTimeMillis();
		}
		
		StringBuilder sqlBuilder = new StringBuilder("FROM MeterLog where uid='" + uid + "' and dt <= " + to + " and dt >= " + from + " order by dt asc ");
		
		Query query = em.createQuery(sqlBuilder.toString());
		return query.getResultList();
	}
}
