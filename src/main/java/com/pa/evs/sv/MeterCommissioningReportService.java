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
	Object getOrNewP2Job(String jobName);
	void saveP2Job(P2JobDto dto);
	void save(List<MeterCommissioningReportDto> dtos);
	void deleteP2Job(String jobNo);
}
