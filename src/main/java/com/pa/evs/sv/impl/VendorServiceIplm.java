package com.pa.evs.sv.impl;

import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Vendor;
import com.pa.evs.repository.VendorRepository;
import com.pa.evs.sv.VendorService;

@Service
@Transactional
public class VendorServiceIplm implements VendorService {

	@Autowired
	VendorRepository vendorRepository;

	@PostConstruct
	public void init() {
		Vendor vendor = vendorRepository.findTopByOrderByIdDesc();
		if (vendor == null) {
			vendor = new Vendor();
			vendor.setName("Default");
			vendorRepository.save(vendor);
		}
	}

	@Override
	public List<Vendor> getVendors() {
		return vendorRepository.findAll();
	}

}
