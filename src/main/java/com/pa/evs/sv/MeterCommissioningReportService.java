package com.pa.evs.sv;

import java.util.List;

import com.pa.evs.dto.MeterCommissioningReportDto;
import com.pa.evs.dto.P2JobDto;
import com.pa.evs.dto.PaginDto;

public interface MeterCommissioningReportService {
	
	void save(MeterCommissioningReportDto dto);
	MeterCommissioningReportDto getLastSubmit(String uid, String msn);
	void search(PaginDto<MeterCommissioningReportDto> pagin);
	void searchP1OnlineTest(PaginDto<? extends Object> pagin);
	Object getOrNewP2Job(String jobName, String hasSubmitReport, String worker);
	void saveP2Job(P2JobDto dto);
	void save(List<MeterCommissioningReportDto> dtos);
	void deleteP2Job(String jobNo);
	Object getP2Jobs(String hasSubmitReport, String msn, String worker);
	void addP2Worker(String manager, List<String> workers);
	List<Object> getP2WorkerByManager(String manager);
	List<Object> getP2Managers();
	void deleteP2Worker(String manager, String worker);
}
