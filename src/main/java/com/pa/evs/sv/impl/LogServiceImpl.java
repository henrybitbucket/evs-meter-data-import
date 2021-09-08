package com.pa.evs.sv.impl;

import com.pa.evs.model.Log;
import com.pa.evs.model.MeterLog;
import com.pa.evs.repository.LogRepository;
import com.pa.evs.repository.MeterLogRepository;
import com.pa.evs.sv.LogService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class LogServiceImpl implements LogService {

    @Autowired
    LogRepository logRepository;

    @Autowired
    MeterLogRepository meterLogRepository;

    @Autowired
    EntityManager em;
    
    @Override
    public List<Log> getRelatedLogs(Map<String, Object> map) {
        String uid = (String) map.get("uid");
        String msn = (String) map.get("msn");
        String ptype = (String) map.get("ptype");
        String midString = (String) map.get("mid");
        
        if (StringUtils.isNotBlank(ptype)) {
            return logRepository.getRelatedLogsWithPtype(uid, msn, ptype);
        }
        if (StringUtils.isNotBlank(midString)) {
            Long mid = Long.parseLong(midString);
            return logRepository.getRelatedLogsFilterMid(uid, msn, mid);
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

    @Override
    public String getKwh(String uid, Map<String, Object> map) throws Exception {

        double result;
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String p1 = (String) map.get("p1");
        String p2 = (String) map.get("p2");
        Calendar curr = Calendar.getInstance();
        curr.set(Calendar.DATE, 1);
        Long from = curr.getTimeInMillis();
        Long to = System.currentTimeMillis();
        if (!p2.equals("0")) {
            Date date = sdf.parse(p2);
            to = date.getTime();
        }

        if (!p1.equals("0")) {
            Date date = sdf.parse(p1);
            from = date.getTime();
        }
        List<MeterLog> list = meterLogRepository.getMeterList(uid, from, to);
        result = list.stream().mapToDouble(m -> Double.parseDouble(m.getKwh())).sum();
        return String.valueOf(result);

    }
}
