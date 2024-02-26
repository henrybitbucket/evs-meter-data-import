package com.pa.evs.sv;

import com.pa.evs.dto.DMSWorkOrdersDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.exception.ApiException;

public interface WorkOrdersService {

	void save(DMSWorkOrdersDto dto) throws ApiException;

	void search(PaginDto<DMSWorkOrdersDto> pagin);

	void delete(Long id) throws ApiException;

	void update(DMSWorkOrdersDto dto) throws ApiException;
}
