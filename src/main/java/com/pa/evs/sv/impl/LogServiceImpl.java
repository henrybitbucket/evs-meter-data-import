package com.pa.evs.sv.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pa.evs.dto.GroupDto;
import com.pa.evs.dto.LogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.model.Group;
import com.pa.evs.model.Log;
import com.pa.evs.model.MeterLog;
import com.pa.evs.model.PiLog;
import com.pa.evs.repository.LogRepository;
import com.pa.evs.repository.MeterLogRepository;
import com.pa.evs.repository.PiLogRepository;
import com.pa.evs.sv.LogService;
import com.pa.evs.utils.Utils;

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
    public PaginDto<LogDto> getRelatedLogs(PaginDto<LogDto> pagin) throws ParseException {
        
        Map<String, Object> map = pagin.getOptions();
        
        String uid = (String) map.get("uid");
        String msn = (String) map.get("msn");
        String ptype = (String) map.get("ptype");
        String type = (String) map.get("type");
        String topic = (String) map.get("topic");
        String midString = (String) map.get("mid");
        Long fromDate = (Long) map.get("fromDate");
        Long toDate = (Long) map.get("toDate");
        Object piId = (Object) map.get("piId");
        Object groupId = (Object) map.get("groupId");
        
        Object markView = (Object) map.get("markView");
        
        String batchId = (String) map.get("batchId");
        
        
        Number repStatus = (Number) map.get("repStatus");
        
        StringBuilder sqlBuilder = new StringBuilder(piId != null ? " Select l, pl, cl " : " Select l, true, cl ");
        
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(l.id) ");
        
        StringBuilder sqlCommonBuilder = new StringBuilder("FROM Log l ");
        
        if (piId != null) {
        	sqlCommonBuilder.append(" JOIN PiLog pl on (pl.pi.id = " + piId + " and l.id = pl.publishLogId) ");
        }
        
        sqlCommonBuilder.append(" LEFT JOIN CARequestLog cl on (cl.msn = cl.msn and cl.uid = l.uid)");
        
        sqlCommonBuilder.append(" WHERE 1=1 ");

        if (uid != null) {
        	sqlCommonBuilder.append(" AND l.uid = '" + uid + "' ");
        }
        if (groupId instanceof Number) {
        	sqlCommonBuilder.append(" AND cl.group.id = " + groupId + " ");
        }
        if (StringUtils.isNotBlank(batchId)) {
        	sqlCommonBuilder.append(" AND (l.batchId = '" + batchId + "' or exists(select l1.id from Log l1 where l1.batchId = '" + batchId + "' and l1.pType = l.pType and l1.msn = l.msn and l1.mid = l.oid )) ");
        }
        if (StringUtils.isNotBlank(msn)) {
        	sqlCommonBuilder.append(" AND l.msn like '%" + msn + "%'");
        } 
        if (StringUtils.isNotBlank(type)) {
        	sqlCommonBuilder.append(" AND l.type = '" + type + "'");
        } 
        if (StringUtils.isNotBlank(topic)) {
        	sqlCommonBuilder.append(" AND l.topic = '" + topic + "'");
        } 
        if (StringUtils.isNotBlank(ptype)) {
        	if (ptype.trim().startsWith("(") && ptype.trim().endsWith(")")) {
        		sqlCommonBuilder.append(" AND l.pType IN " + ptype.toUpperCase() + "");
        	} else {
        		sqlCommonBuilder.append(" AND upper(l.pType) like '%" + ptype.toUpperCase() + "%'");
        	}
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
        		// sqlCommonBuilder.append(" AND (l.repStatus = " + repStatus + " OR (l.repStatus is not null and l.repStatus <> 0)) and l.msn <> '' ");
        		sqlCommonBuilder.append(" AND (l.repStatus = " + repStatus + " OR (l.repStatus is not null and l.repStatus <> 0)) ");
        		sqlCommonBuilder.append(" AND l.mid is not null and l.type = 'PUBLISH' and l.topic <> 'evs/pa/local/data/send' and " + (markView instanceof Number ? "(l.markView = " + markView + ") " : "(l.markView is null or l.markView <> 1) "));	
        	} else {
        		// sqlCommonBuilder.append(" AND l.repStatus = " + repStatus + "  and l.msn <> '' ");
        		sqlCommonBuilder.append(" AND l.repStatus = " + repStatus + " ");
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
        		LogDto dto = LogDto.builder()
	              .id(l.getId())
	              .createDate(l.getCreateDate())
	              .mid(l.getMid())
	              .uid(l.getUid())
	              .gid(l.getGid())
	              .msn(l.getMsn())
	              .sig(l.getSig())
	              .topic(l.getTopic())
	              .type(l.getType())
	              .repStatus(l.getRepStatus())
	              .batchId(l.getBatchId())
	              .markView(l.getMarkView())
	              .pType(l.getPType())
	              .pId(l.getPId())
	              .sn(l.getSn()) 
	              .RepStatusDesc(l.getRepStatusDesc())
	              .raw(l.getRaw())
	              .build();
        		if (os[1] instanceof PiLog) {
	        		PiLog pl = (PiLog) os[1];	        		
	        		dto.setFtpResStatus(pl.getFtpResStatus());
        		}
        		if (os[2] instanceof CARequestLog) {
        			CARequestLog cl = (CARequestLog) os[2];       		
        			dto.setAddress(Utils.formatHomeAddress(cl));
        			Group group = cl.getGroup();
        			if (group != null) {
        				dto.setGroupDto(GroupDto.builder()
	                    .id(group.getId())
	                    .name(group.getName())
	                    .remark(group.getRemark())
	                    .build());
        			} else {
        				dto.setGroupDto(new GroupDto());
        			}        		
        			dto.setSn(cl.getSn());
        		}
        		pagin.getResults().add(dto);
        	});
        } else {
        	pagin.setResults((List<LogDto>)data);
        }
        
        return pagin;   
    }
    
    @Override
    public void searchLog(PaginDto<LogDto> pagin) {
    	
    	Map<String, Object> map = pagin.getOptions();
    	
    	Long fromDate = (Long) map.get("fromDate");
        Long toDate = (Long) map.get("toDate");
        String userName = (String) map.get("userName");
		
		StringBuilder sqlBuilder = new StringBuilder("select l FROM Log l JOIN Users u ON l.user.userId = u.userId");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM Log l JOIN Users u ON l.user.userId = u.userId");

        StringBuilder sqlCommonBuilder = new StringBuilder();
        
        if(userName != null) {
        	sqlCommonBuilder.append(" AND u.username like '%" + userName + "%' ");
        }
        if (fromDate != null) {
            sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM l.createDate) * 1000 >= " + fromDate);
        }
        if (toDate != null) {
            sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM l.createDate) * 1000 <= " + toDate);
        }
        
        if(userName == null && fromDate == null && toDate == null ) {
        	sqlCommonBuilder.append(" WHERE 1=1 ");
        }
        sqlBuilder.append(sqlCommonBuilder);
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
            return;
        }

        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());

        List<Log> list = query.getResultList();

        list.forEach(li -> {
        	if(Objects.isNull(li.getUser())) {
        		LogDto dto = LogDto.builder()
                        .id(li.getId())
                        .mid(li.getMid())
                        .msn(li.getMsn())
                        .oid(li.getOid())
                        .pId(li.getPId())                       
                        .uid(li.getUid())
                        .build();
                pagin.getResults().add(dto);               
        	} else {
        		LogDto dto = LogDto.builder()
                        .id(li.getId())
                        .mid(li.getMid())
                        .msn(li.getMsn())
                        .oid(li.getOid())
                        .pId(li.getPId())                       
                        .uid(li.getUid())
                        .userName(li.getUser().getUsername())
                        .userEmail(li.getUser().getEmail())
                        .build();
                pagin.getResults().add(dto);
        	}
        }); 
       

    }
    
    @Override
	public Object getMeterLog(Map<String, Object> map) {
    	String uid = (String) map.get("uid"); 
    	String order = (String) map.get("order"); 
    	if (StringUtils.isBlank(order)) {
    		order = "asc";
    	}
    	Number from = (Number) map.get("from");
    	Number to = (Number) map.get("to");
    	
    	Number offset = (Number) map.get("offset");
    	Number limit = (Number) map.get("limit");
		
		if (from == null) {
			from = System.currentTimeMillis() - 10 * 24 * 60 * 60 * 1000l;
		}
		
		if (to == null) {
			to = System.currentTimeMillis() + 12 * 60 * 60 * 1000l;
		}
		
		StringBuilder sqlBuilder = new StringBuilder("FROM MeterLog where uid='" + uid + "' and dt <= " + to + " and dt >= " + from + " order by dt " + order);
		
		Query query = em.createQuery(sqlBuilder.toString());
		if (offset != null && offset.intValue() >= 0) {
			query.setFirstResult(offset.intValue());
		}
		if (limit != null && limit.intValue() > 0) {
			query.setMaxResults(limit.intValue());
		}
		@SuppressWarnings("unchecked")
		List<MeterLog> logs = query.getResultList();
		SimpleDateFormat sf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
		logs.forEach(ml -> {
			ml.setMDt(ml.getDtd() != null ? sf.format(ml.getDtd()) : null);
		});
		return logs;
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
