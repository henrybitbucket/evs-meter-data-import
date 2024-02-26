package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.DMSLocationLock;


@Transactional
@Repository
public interface DMSLocationLockRepository extends JpaRepository<DMSLocationLock, Long> {

	Optional<DMSLocationLock> findByLockId(Long lockId);
	Optional<DMSLocationLock> findByLockIdAndLocationKey(Long lockId, String locationKey);
	
}
