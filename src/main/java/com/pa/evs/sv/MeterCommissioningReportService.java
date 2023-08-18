package com.pa.evs.sv;

import com.pa.evs.dto.MeterCommissioningReportDto;
import com.pa.evs.dto.PaginDto;

public interface MeterCommissioningReportService {
	
	void save(MeterCommissioningReportDto dto);
	MeterCommissioningReportDto getLastSubmit(String uid, String msn);
	void search(PaginDto<MeterCommissioningReportDto> pagin);
	void searchP1OnlineTest(PaginDto<? extends Object> pagin);
}
