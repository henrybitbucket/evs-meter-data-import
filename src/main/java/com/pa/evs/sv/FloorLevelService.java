package com.pa.evs.sv;

import com.pa.evs.dto.FloorLevelDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.exception.ApiException;

public interface FloorLevelService {

	void save(FloorLevelDto floorLevel) throws ApiException;

	void search(PaginDto<FloorLevelDto> pagin);

	void delete(Long id) throws ApiException;

	void update(FloorLevelDto dto) throws ApiException;
}
