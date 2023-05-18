package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Vendor;

@Repository
@Transactional
public interface VendorRepository extends JpaRepository<Vendor, Long> {

	Vendor findTopByOrderByIdDesc();

	Vendor findByName(String string);
}
