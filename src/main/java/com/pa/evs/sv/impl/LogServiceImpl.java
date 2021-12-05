package com.pa.evs.sv.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.Log;
import com.pa.evs.model.MeterLog;
import com.pa.evs.model.PiLog;
import com.pa.evs.repository.LogRepository;
import com.pa.evs.repository.MeterLogRepository;
import com.pa.evs.repository.PiLogRepository;
import com.pa.evs.sv.LogService;

@Service
public class LogServiceImpl implements LogService {

    @Autowired
    LogRepository logRepository;
    
    @Autowired
    PiLogRepository piLogRepository;

    @Autowired
    MeterLogRepository meterLogRepository;

    @Autowired
    EntityManager em;
    
    @SuppressWarnings("unchecked")
    @Override
    public PaginDto<Log> getRelatedLogs(PaginDto<Log> pagin) throws ParseException {
        
        Map<String, Object> map = pagin.getOptions();
        
        String uid = (String) map.get("uid");
        String msn = (String) map.get("msn");
        String ptype = (String) map.get("ptype");
        String midString = (String) map.get("mid");
        Long fromDate = (Long) map.get("fromDate");
        Long toDate = (Long) map.get("toDate");
        Object piId = (Object) map.get("piId");
        
        String batchId = (String) map.get("batchId");
        
        
        Number repStatus = (Number) map.get("repStatus");
        
        StringBuilder sqlBuilder = new StringBuilder(piId != null ? " Select l, pl " : " Select l ");
        
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(l.id) ");
        
        StringBuilder sqlCommonBuilder = new StringBuilder("FROM Log l");
        
        if (piId != null) {
        	sqlCommonBuilder.append(" JOIN PiLog pl on (pl.pi.id = " + piId + " and l.msn = pl.msn and l.mid = pl.mid and l.type = 'PUBLISH') ");
        }
        
        sqlCommonBuilder.append(" WHERE 1=1 ");

        if (uid != null) {
        	sqlCommonBuilder.append(" AND l.uid = '" + uid + "' ");
        }
        if (StringUtils.isNotBlank(batchId)) {
        	sqlCommonBuilder.append(" AND (l.batchId = '" + batchId + "' or exists(select l1.id from Log l1 where l1.batchId = '" + batchId + "' and l1.pType = l.pType and l1.msn = l.msn and l1.mid = l.oid )) ");
        }
        if (msn != null) {
        	sqlCommonBuilder.append(" AND l.msn = '" + msn + "'");
        }        
        if (StringUtils.isNotBlank(ptype)) {
            sqlCommonBuilder.append(" AND upper(l.pType) like '%" + ptype.toUpperCase() + "%'");
        }
        if (StringUtils.isNotBlank(midString)) {
            sqlCommonBuilder.append(" AND (l.mid = " + midString + " OR l.oid = " + midString + " OR l.rmid = " + midString + ")");
        }
        if (fromDate != null) {
            sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM l.createDate) * 1000 >= " + fromDate);
        }
        if (toDate != null) {
            sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM l.createDate) * 1000 <= " + toDate);
        }
        if (repStatus != null) {
        	if (repStatus.intValue() == -999) {
        		sqlCommonBuilder.append(" AND (l.repStatus = " + repStatus + " OR (l.repStatus is not null and l.repStatus <> 0)) and l.msn <> '' ");
        		sqlCommonBuilder.append(" AND l.mid is not null and l.type = 'PUBLISH' and l.topic <> 'evs/pa/local/data/send' and (l.markView is null or l.markView <> 1) ");	
        	} else {
        		sqlCommonBuilder.append(" AND l.repStatus = " + repStatus + "  and l.msn <> '' ");
        	}
        	
        }
        
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY l.createDate DESC");
        sqlCountBuilder.append(sqlCommonBuilder);
        
        if (pagin.getOffset() == null || pagin.getOffset() < 0) {
            pagin.setOffset(0);
        }
        
        if (pagin.getLimit() == null || pagin.getLimit() <= 0) {
            pagin.setLimit(100);
        }
        
        Query queryCount = em.createQuery(sqlCountBuilder.toString());
        
        Long count = ((Number)queryCount.getSingleResult()).longValue();
        pagin.setTotalRows(count);
        
        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());
        
        @SuppressWarnings("rawtypes")
		List data = query.getResultList();
        if (!data.isEmpty() && data.get(0) instanceof Object[]) {
        	pagin.setResults(new ArrayList<>());
        	data.forEach(obj -> {
        		Object[] os = (Object[]) obj;
        		Log l = (Log) os[0];
        		PiLog pl = (PiLog) os[1];
        		l.setFtpResStatus(pl.getFtpResStatus());
        		pagin.getResults().add(l);
        	});
        } else {
        	pagin.setResults((List<Log>)data);
        }
        
        if (!pagin.getResults().isEmpty()) {
	        query = em.createQuery("SELECT sn, msn FROM CARequestLog where msn in (:msn)");
	        query.setParameter("msn", pagin.getResults().stream().map(l -> l.getMsn()).collect(Collectors.toList()));
	        List<Object[]> objs = query.getResultList();
	        Map<String, String> temp = new LinkedHashMap<>();
	        objs.forEach(obj -> temp.put((String)obj[1], (String)obj[0]));
	        pagin.getResults().forEach(l -> l.setSn(temp.get(l.getMsn())));
	        temp.clear();
        }
        
        return pagin;
        
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
