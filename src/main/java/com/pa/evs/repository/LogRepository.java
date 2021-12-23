package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Log;

@Transactional
public interface LogRepository extends JpaRepository<Log, Long> {

	@Modifying
	@Query(value = "CREATE SEQUENCE IF NOT EXISTS {h-schema}mid_sequence", nativeQuery = true)
	void createMIDSeq();

	@Query(value = "select nextval('{h-schema}mid_sequence')", nativeQuery = true)
	Number nextvalMID();

	@Modifying
	@Query(value = "ALTER SEQUENCE {h-schema}mid_sequence RESTART WITH 10000", nativeQuery = true)
	void nextvalMID(Long lastValue);

	@Modifying
	@Query(value = "UPDATE {h-schema}log set rep_status = ?1 where mid = ?2 and type = 'PUBLISH'", nativeQuery = true)
	void updateStatus(Long status, Long mId);

	List<Log> findByMsnAndMid(String msn, Long mId);

}
