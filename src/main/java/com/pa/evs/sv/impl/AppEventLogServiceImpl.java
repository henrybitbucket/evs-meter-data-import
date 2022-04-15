package com.pa.evs.sv.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.AppEventLog;
import com.pa.evs.repository.AppEventLogRepository;
import com.pa.evs.sv.AppEventLogService;


@Transactional
@Service
public class AppEventLogServiceImpl implements AppEventLogService {
	
	@Autowired
	AppEventLogRepository appEventLogRepository;

	@Override
	public void saveStartTime() {
		// TODO Auto-generated method stub
		AppEventLog appEventLog = new AppEventLog();
		appEventLog.setAction("START");
		appEventLogRepository.save(appEventLog);
		
	}

	@Override
	public void saveStopTime() {
		// TODO Auto-generated method stub
		AppEventLog appEventLog = new AppEventLog();
		appEventLog.setAction("STOP");
		appEventLogRepository.save(appEventLog);
	}		
}
