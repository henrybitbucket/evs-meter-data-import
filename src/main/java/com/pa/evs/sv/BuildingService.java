package com.pa.evs.sv;

import com.pa.evs.dto.BuildingDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.exception.ApiException;

public interface BuildingService {

	void search(PaginDto<BuildingDto> pagin);

	void delete(Long id) throws ApiException;

	void update(BuildingDto building) throws ApiException;

	void save(BuildingDto dto) throws ApiException;
}
