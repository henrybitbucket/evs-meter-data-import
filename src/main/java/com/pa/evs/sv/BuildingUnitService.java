package com.pa.evs.sv;

import com.pa.evs.dto.BuildingUnitDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.exception.ApiException;

public interface BuildingUnitService {

	void search(PaginDto<BuildingUnitDto> pagin);

	void save(BuildingUnitDto dto) throws ApiException;

	void update(BuildingUnitDto dto) throws ApiException;

	void delete(Long id) throws ApiException;

}
