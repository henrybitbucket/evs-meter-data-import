package com.pa.evs.sv.impl;

import com.pa.evs.dto.PaginDto;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
public class LogServiceImpl implements LogService {

    @Autowired
    LogRepository logRepository;

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
        
        Number repStatus = (Number) map.get("repStatus");
        
        StringBuilder sqlBuilder = new StringBuilder("FROM Log");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Log");
        
        StringBuilder sqlCommonBuilder = new StringBuilder();
        sqlCommonBuilder.append(" WHERE 1=1 ");
        
        if (uid != null) {
        	sqlCommonBuilder.append(" AND uid = '" + uid + "' ");
        }
        if (msn != null) {
        	sqlCommonBuilder.append(" AND msn = '" + msn + "'");
        }        
        if (StringUtils.isNotBlank(ptype)) {
            sqlCommonBuilder.append(" AND upper(pType) like '%" + ptype.toUpperCase() + "%'");
        }
        if (StringUtils.isNotBlank(midString)) {
            sqlCommonBuilder.append(" AND (mid = " + midString + " OR oid = " + midString + " OR rmid = " + midString + ")");
        }
        if (fromDate != null) {
            sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM createDate) * 1000 >= " + fromDate);
        }
        if (toDate != null) {
            sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM createDate) * 1000 <= " + toDate);
        }
        if (repStatus != null) {
        	sqlCommonBuilder.append(" AND repStatus = " + repStatus + " ");
        	if (repStatus.intValue() == -999) {
        		sqlCommonBuilder.append(" and mid is not null and type = 'PUBLISH' and (mark_view is null or mark_view <> 1) ");	
        	}
        	
        }
        
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY createDate DESC");
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
        pagin.setResults(new ArrayList<>());
        if (count == 0l) {
            return pagin;
        }
        
        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());
        
        pagin.setResults(query.getResultList());
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
