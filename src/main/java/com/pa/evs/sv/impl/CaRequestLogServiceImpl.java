package com.pa.evs.sv.impl;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.pa.evs.model.CARequestLog;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.sv.CaRequestLogService;

@Component
@SuppressWarnings("unchecked")
public class CaRequestLogServiceImpl implements CaRequestLogService {
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(CommonServiceImpl.class);
	
	@Autowired
	private CARequestLogRepository caRequestLogRepository;

	@Override
	public Optional<CARequestLog> findByUid(String uid) {
		return caRequestLogRepository.findByUid(uid);
	}
	
}
