package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Firmware;

@Repository
@Transactional
public interface FirmwareRepository extends JpaRepository<Firmware, Long>{
    
	@Query(value = "SELECT * FROM {h-schema}firmware_tbl fw join {h-schema}vendor v on v.id = fw.vendor_id "
			+ " WHERE fw.id IN (SELECT MAX(fw1.id) FROM {h-schema}firmware_tbl fw1 WHERE fw1.vendor_id = v.id GROUP BY fw1.vendor_id)", nativeQuery = true)
	List<Firmware> findTopByVendorOrderByIdDesc();
    
	@Modifying
	@Query(value = "update {h-schema}firmware_tbl set vendor_id = ?1 where vendor_id is null", nativeQuery = true)
	void updateVendor(Long vendorId);
	
	List<Firmware> findByVersionAndVendorId(String version, Long vendorId);
}
