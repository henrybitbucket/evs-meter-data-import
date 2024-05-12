package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.DMSLockVendor;

@Transactional
@Repository
public interface DMSLockVendorRepository extends JpaRepository<DMSLockVendor, Long> {

	Optional<DMSLockVendor> findByName(String string);

	Optional<DMSLockVendor> findByLabel(String label);

}
