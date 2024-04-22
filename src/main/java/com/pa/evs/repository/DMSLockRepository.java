package com.pa.evs.repository;

import com.pa.evs.model.DMSLock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
@Repository
public interface DMSLockRepository extends JpaRepository<DMSLock, Long> {

	Optional<DMSLock> findByOriginalIdAndLockNumber(String lockId, String lockNumber);
	Optional<DMSLock> findByLockNumber(String lockNumber);
	Optional<DMSLock> findByLockBid(String lockBid);

}
