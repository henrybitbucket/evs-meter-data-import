package com.pa.evs.sv;

import com.pa.evs.dto.DMSLocationSiteDto;
import com.pa.evs.dto.DMSSiteDto;
import com.pa.evs.dto.PaginDto;
import com.pa.evs.exception.ApiException;

@SuppressWarnings("rawtypes")
public interface DMSSiteService {

	void save(DMSSiteDto dto) throws ApiException;

	void search(PaginDto<DMSSiteDto> pagin);

	void delete(Long id) throws ApiException;

	void update(DMSSiteDto dto) throws ApiException;

	void searchWorkOrders(PaginDto pagin);

	void searchLocations(PaginDto pagin);

	void linkLocation(DMSLocationSiteDto dto);

	void unLinkLocation(Long linkSiteLocationId);
}
