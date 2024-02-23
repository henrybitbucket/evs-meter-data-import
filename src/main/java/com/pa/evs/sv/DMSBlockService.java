package com.pa.evs.sv;

import com.pa.evs.dto.BlockDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.exception.ApiException;

public interface DMSBlockService {

	void save(BlockDto floorLevel) throws ApiException;

	void search(PaginDto<BlockDto> pagin);

	void delete(Long id) throws ApiException;

	void update(BlockDto dto) throws ApiException;
}
