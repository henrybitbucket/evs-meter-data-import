package com.pa.evs.sv;

import com.pa.evs.dto.CompanyDto;
import com.pa.evs.dto.PaginDto;

public interface CompanyService {

	void save(CompanyDto dto);

	void delete(Long cpnId);

	PaginDto<CompanyDto> search(PaginDto<CompanyDto> pagin);

}
