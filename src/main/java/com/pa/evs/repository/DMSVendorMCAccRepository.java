package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.DMSLockVendor;
import com.pa.evs.model.DMSVendorMCAcc;

@Repository
public interface DMSVendorMCAccRepository extends JpaRepository<DMSVendorMCAcc, Long> {

	List<DMSVendorMCAcc> findByVendor(DMSLockVendor vendor);

	@Modifying
	@Query("DELETE DMSVendorMCAcc WHERE vendor.id = ?1 and mcAcc.id in (?2)")
	void deleteByVendorAndMcAccIn(Long vendorId, List<Long> deleteList);

	@Modifying
	void deleteByVendor(DMSLockVendor vendor);

}
