package com.pa.evs.sv;

import com.pa.evs.dto.DMSWorkOrdersDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.exception.ApiException;
import com.pa.evs.model.DMSWorkOrders;

public interface WorkOrdersService {

	DMSWorkOrders save(DMSWorkOrdersDto dto) throws ApiException;

	void search(PaginDto<DMSWorkOrdersDto> pagin);

	void delete(Long id) throws ApiException;

	DMSWorkOrders update(DMSWorkOrdersDto dto) throws ApiException;
}
