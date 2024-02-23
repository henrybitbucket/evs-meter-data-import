package com.pa.evs.sv;

import com.pa.evs.dto.BuildingDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.exception.ApiException;

public interface DMSBuildingService {

	void search(PaginDto<BuildingDto> pagin, String search);

	void delete(Long id) throws ApiException;

	void update(BuildingDto building) throws ApiException;

	void save(BuildingDto dto) throws ApiException;

//	void updateBuildingFullText();
}
