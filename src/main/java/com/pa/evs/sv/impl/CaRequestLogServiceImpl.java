package com.pa.evs.sv.impl;

import com.pa.evs.constant.Message;
import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.sv.CaRequestLogService;
import com.pa.evs.utils.CsvUtils;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

@Component
@SuppressWarnings("unchecked")
public class CaRequestLogServiceImpl implements CaRequestLogService {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(EVSPAServiceImpl.class);
	
	@Autowired
	private CARequestLogRepository caRequestLogRepository;
	
	@Autowired
	EntityManager em;

    private List<String> cacheCids = Collections.EMPTY_LIST;

	@PostConstruct
    public void init() {
        LOG.debug("Loading CID into cache");
        cacheCids = caRequestLogRepository.getCids();
    }

	@Override
	public Optional<CARequestLog> findByUid(String uid) {
		return caRequestLogRepository.findByUid(uid);
	}

    @Override
    public void save(CaRequestLogDto dto) throws Exception {
        CARequestLog ca = null;
        Calendar c = Calendar.getInstance();
        if (StringUtils.isNotBlank(dto.getMsn())) {
        	dto.setMsn(dto.getMsn().trim());
        }
        if (dto.getId() != null) {
            Optional<CARequestLog> opt = caRequestLogRepository.findById(dto.getId());
            if (opt.isPresent()) {
                ca = opt.get();
                
                if (StringUtils.isNotBlank(dto.getMsn()) && !dto.getMsn().equalsIgnoreCase(ca.getMsn()) && BooleanUtils.isTrue(caRequestLogRepository.existsByMsn(dto.getMsn()))) {
                    throw new Exception(Message.MSN_WAS_ASSIGNED);
                }
                ca.setModifyDate(c.getTime());
            } else {
                ca = new CARequestLog();
                ca.setCreateDate(c.getTime());
            }
        } else {
            ca = new CARequestLog();
            ca.setCreateDate(c.getTime());
        }
        
        ca.setCertificate(dto.getCertificate());
        ca.setMsn(dto.getMsn());
        ca.setRequireRefresh(dto.getRequireRefresh());
        ca.setRaw(dto.getRaw());
        ca.setStartDate(dto.getStartDate());
        ca.setEndDate(dto.getEndDate());
        caRequestLogRepository.save(ca);
        
    }

    @Override
    public PaginDto<CARequestLog> search(PaginDto<CARequestLog> pagin) {
        StringBuilder sqlBuilder = new StringBuilder("FROM CARequestLog");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM CARequestLog");
        
        StringBuilder sqlCommonBuilder = new StringBuilder();
        if (CollectionUtils.isEmpty(pagin.getOptions())) {
            sqlCommonBuilder.append(" WHERE 1=1 ");
        } else {
            
            Map<String, Object> options = pagin.getOptions();
            Long fromDate = (Long) options.get("fromDate");
            Long toDate = (Long) options.get("toDate");
            String status = (String) options.get("status");
            String querySn = (String) options.get("querySn");
            String queryMsn = (String) options.get("queryMsn");
            String querySnOrCid = (String) options.get("querySnOrCid");
            List<String> cids = (List<String>) options.get("selectedCids");
            
            sqlCommonBuilder.append(" WHERE ");
            
            if (StringUtils.isNotBlank(querySn)) {
                sqlCommonBuilder.append(" upper(sn) like '%" + querySn.toUpperCase() + "%' AND ");
            }
            if (StringUtils.isNotBlank(queryMsn)) {
                sqlCommonBuilder.append(" msn like '%" + queryMsn + "%' AND ");
            }
            if (StringUtils.isNotBlank(querySnOrCid)) {
                sqlCommonBuilder.append(" (lower(sn) like '%" + querySnOrCid.toLowerCase().trim() + "%' or lower(cid) like '%" + querySnOrCid.toLowerCase().trim() + "%') AND ");
            }
                
            if (StringUtils.isNotBlank(status)) {
                sqlCommonBuilder.append(" ( ");
                if (status.equals("create_date")) {
                    sqlCommonBuilder.append(" status = 0");
                    sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM createDate) * 1000 >= " + fromDate);
                    sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM createDate) * 1000 <= " + toDate);
                }
                if (status.equals("activate_date")) {
                    sqlCommonBuilder.append(" status = 1");
                    sqlCommonBuilder.append(" AND activateDate >= " + fromDate);
                    sqlCommonBuilder.append(" AND activateDate <= " + toDate);
                }
                sqlCommonBuilder.append(" ) ");
            } else {
                if (fromDate != null && toDate == null) {
                    sqlCommonBuilder.append(" (( ");
                    sqlCommonBuilder.append(" EXTRACT(EPOCH FROM createDate) * 1000 >= " + fromDate);
                    sqlCommonBuilder.append(" OR ");
                    sqlCommonBuilder.append(" activateDate >= " + fromDate);
                    sqlCommonBuilder.append(" )) ");
                } else if (fromDate == null && toDate != null) {
                    sqlCommonBuilder.append(" (( ");
                    sqlCommonBuilder.append(" EXTRACT(EPOCH FROM createDate) * 1000 <= " + toDate);
                    sqlCommonBuilder.append(" OR ");
                    sqlCommonBuilder.append(" activateDate <= " + toDate);
                    sqlCommonBuilder.append(" )) ");
                } else if (fromDate != null && toDate != null) {
                    sqlCommonBuilder.append(" (( ");
                    sqlCommonBuilder.append(" EXTRACT(EPOCH FROM createDate) * 1000 >= " + fromDate);
                    sqlCommonBuilder.append(" AND EXTRACT(EPOCH FROM createDate) * 1000 <= " + toDate + " ) OR ( ");
                    sqlCommonBuilder.append(" activateDate >= " + fromDate);
                    sqlCommonBuilder.append(" AND activateDate <= " + toDate);
                    sqlCommonBuilder.append(" )) ");
                } else if (fromDate == null && toDate == null) {
                    sqlCommonBuilder.append(" 1 = 1 ");
                }
            }
            
            if (!CollectionUtils.isEmpty(cids)) {
                sqlCommonBuilder.append(" AND (cid = '" + cids.get(0) + "'");
                for (int i = 1; i < cids.size(); i++) {
                    sqlCommonBuilder.append(" OR cid = '" + cids.get(i) + "'");
                }
                sqlCommonBuilder.append(" ) ");
            }
        }
        
        sqlBuilder.append(sqlCommonBuilder).append(" ORDER BY id asc");
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

    @Transactional
	@Override
	public void linkMsn(Map<String, Object> map) {
    	
    	if (map.get("sn") != null) {
            if (BooleanUtils.isTrue(caRequestLogRepository.existsByMsn((String)map.get("msn")))) {
                throw new RuntimeException("Invalid MSN, MSN is being linked !");
            }
    		caRequestLogRepository.linkMsnBySn((String)map.get("sn"), (String)map.get("msn"));
    	} else if (map.get("uuid") != null) {
    		caRequestLogRepository.linkMsn((String)map.get("uuid"), (String)map.get("msn"));
    	}

	}

    @Override
    public File downloadCsv(List<CARequestLog> listInput, Long activateDate) throws IOException {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
        String tag = sdf.format(new Date());
        String fileName = "ca_request_log-" + tag + ".csv";
        return CsvUtils.writeCaRequestLogCsv(listInput, fileName, activateDate);
    }
    
    @Override
    public List<String> getCids(boolean refresh) {
	    if (refresh) {
	        cacheCids = caRequestLogRepository.getCids();
        }
	    return cacheCids;
    }
}
