package com.pa.evs.sv.impl;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Vendor;
import com.pa.evs.repository.CARequestLogRepository;
import com.pa.evs.repository.FirmwareRepository;
import com.pa.evs.repository.VendorRepository;
import com.pa.evs.sv.VendorService;

@Service
@Transactional
public class VendorServiceIplm implements VendorService {

	@Autowired
	VendorRepository vendorRepository;
	
	@Autowired
	CARequestLogRepository caRequestLogRepository;
	
	@Autowired
	FirmwareRepository firmwareRepository;

	@PostConstruct
	public void init() {
		Vendor vendor = vendorRepository.findByName("Default");
		if (vendor == null) {
			vendor = new Vendor();
			vendor.setName("Default");
			vendor = vendorRepository.save(vendor);
		}
		caRequestLogRepository.updateVendor(vendor.getId());
		firmwareRepository.updateVendor(vendor.getId());
	}

	@Override
	public List<Vendor> getVendors() {
		return vendorRepository.findAll();
	}

}
