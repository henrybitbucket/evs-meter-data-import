package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.DMSLock;

@Transactional
@Repository
public interface DMSLockRepository extends JpaRepository<DMSLock, Long> {

	Optional<DMSLock> findByOriginalIdAndLockNumber(String lockId, String lockNumber);

}
