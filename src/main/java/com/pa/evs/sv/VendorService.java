package com.pa.evs.sv;

import java.util.List;

import org.springframework.stereotype.Repository;

import com.pa.evs.model.Vendor;

@Repository
public interface VendorService {

	List<Vendor> getVendors();

}
