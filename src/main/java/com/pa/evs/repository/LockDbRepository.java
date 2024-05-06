package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.LockDb;

public interface LockDbRepository extends JpaRepository<LockDb, Long> {

	@Query(value = "select * from pa_evs_db.lock_db where name = ?1 for update skip locked", nativeQuery = true)
	Optional<LockDb> findByNameForUpdate(String name);
	
	Optional<LockDb> findByName(String name);
	
}
