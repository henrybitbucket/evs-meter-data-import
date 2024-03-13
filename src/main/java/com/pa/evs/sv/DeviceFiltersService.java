package com.pa.evs.sv;

import java.util.List;

import com.pa.evs.dto.DeviceFiltersDto;

public interface DeviceFiltersService {

	List<DeviceFiltersDto> getDeviceFilters();

	void saveDeviceFilters(DeviceFiltersDto filters);

}
