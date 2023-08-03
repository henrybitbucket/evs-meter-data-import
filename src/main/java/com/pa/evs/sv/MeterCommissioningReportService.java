package com.pa.evs.sv;

import com.pa.evs.dto.MeterCommissioningReportDto;

public interface MeterCommissioningReportService {
	
	void save(MeterCommissioningReportDto dto);
	MeterCommissioningReportDto getLastSubmit(String uid, String msn);
}
