package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.DMSVendorMCAcc;
import com.pa.evs.model.Vendor;

public interface DMSVendorMCAccRepository extends JpaRepository<DMSVendorMCAcc, Long> {

	List<DMSVendorMCAcc> findByVendor(Vendor vendor);

}
