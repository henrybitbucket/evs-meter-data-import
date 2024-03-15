package com.pa.evs.sv.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.pa.evs.dto.DeviceFiltersDto;
import com.pa.evs.model.DeviceFilters;
import com.pa.evs.model.Users;
import com.pa.evs.repository.DeviceFiltersRepository;
import com.pa.evs.repository.UserRepository;
import com.pa.evs.sv.DeviceFiltersService;
import com.pa.evs.utils.SecurityUtils;

@Service
public class DeviceFiltersServiceImpl implements DeviceFiltersService {
	
	@Autowired
	private DeviceFiltersRepository deviceFiltersRepository;
	
	@Autowired
	private UserRepository userRepository;

	@Override
	public List<DeviceFiltersDto> getDeviceFilters() {
		List<DeviceFiltersDto> results = new ArrayList<>();
		Users loggedInUser = userRepository.findByEmail(SecurityUtils.getEmail());
		List<DeviceFilters> list = deviceFiltersRepository.findByUser(loggedInUser);
		list.forEach(li -> {
			DeviceFiltersDto dto = DeviceFiltersDto.builder()
                        .id(li.getId())
                        .name(li.getName())
                        .filters(li.getFilters())
                        .build();
        	results.add(dto);
        });
		return results;
	}

	@Override
	public void saveDeviceFilters(DeviceFiltersDto filters) {
		String name = filters.getName();
		String filtersStr = filters.getFilters();
		
		Users loggedInUser = userRepository.findByEmail(SecurityUtils.getEmail());
		
		if (StringUtils.isBlank(name)) {
			throw new RuntimeException("Name is required!");
		}
		
		Optional<DeviceFilters> filtersOpt = deviceFiltersRepository.findByNameAndUser(name, loggedInUser);
		if (filtersOpt.isPresent()) {
			filtersOpt.get().setFilters(filtersStr);
			filtersOpt.get().setModifyDate(new Date());
			deviceFiltersRepository.save(filtersOpt.get());
		} else {
			DeviceFilters newFilters = new DeviceFilters();
			newFilters.setFilters(filtersStr);
			newFilters.setName(name);
			newFilters.setUser(loggedInUser);
			deviceFiltersRepository.save(newFilters);
		}
	}

}
