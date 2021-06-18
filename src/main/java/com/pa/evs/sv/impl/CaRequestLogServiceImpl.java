package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.pa.evs.dto.CaRequestLogDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.model.CARequestLog;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.sv.CaRequestLogService;

@Component
@SuppressWarnings("unchecked")
public class CaRequestLogServiceImpl implements CaRequestLogService {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CommonServiceImpl.class);
	
	@Autowired
	private CARequestLogRepository caRequestLogRepository;
	
	@Autowired
	EntityManager em;

	@Override
	public Optional<CARequestLog> findByUid(String uid) {
		return caRequestLogRepository.findByUid(uid);
	}

    @Override
    public void save(CaRequestLogDto dto) {
        CARequestLog ca = null;
        Calendar c = Calendar.getInstance();
        
        if (dto.getId() != null) {
            Optional<CARequestLog> opt = caRequestLogRepository.findById(dto.getId());
            if (opt.isPresent()) {
                ca = opt.get();
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
    public void search(PaginDto<CaRequestLogDto> pagin) {
        StringBuilder sqlBuilder = new StringBuilder("FROM CARequestLog");
        StringBuilder sqlCountBuilder = new StringBuilder("SELECT count(*) FROM CARequestLog");
        
        StringBuilder sqlCommonBuilder = new StringBuilder();
        sqlCommonBuilder.append(" WHERE 1=1 ");
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
            return;
        }
        
        Query query = em.createQuery(sqlBuilder.toString());
        query.setFirstResult(pagin.getOffset());
        query.setMaxResults(pagin.getLimit());
        
        pagin.setResults(query.getResultList());
        
    }
	
}
