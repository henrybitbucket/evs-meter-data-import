package com.pa.evs.repository;

import java.util.List;

import com.pa.evs.model.MeterLog;
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
	
	@Query(value = "SELECT * FROM {h-schema}log WHERE uid = ?1 AND msn = ?2 ORDER BY id DESC LIMIT 20 OFFSET 0", nativeQuery = true)
	List<Log> getRelatedLogs(final String uid, final String msn);
	
	@Query(value = "SELECT * FROM {h-schema}log WHERE uid = ?1 AND msn = ?2 AND p_type = ?3 ORDER BY id DESC LIMIT 20 OFFSET 0", nativeQuery = true)
	List<Log> getRelatedLogsWithPtype(final String uid, final String msn, final String ptype);
	
	@Query(value = "SELECT * FROM {h-schema}log WHERE uid = ?1 AND msn = ?2 AND (mid = ?3 OR oid = ?3 or rmid = ?3)", nativeQuery = true)
	List<Log> getRelatedLogsFilterMid(final String uid, final String msn, final Long mid);

}
