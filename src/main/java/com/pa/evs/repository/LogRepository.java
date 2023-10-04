package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Log;

@Transactional
public interface LogRepository extends JpaRepository<Log, Long> {

//	@Modifying
//	@Query(value = "CREATE SEQUENCE IF NOT EXISTS {h-schema}mid_sequence", nativeQuery = true)
//	void createMIDSeq();
//
//	@Query(value = "select nextval('{h-schema}mid_sequence')", nativeQuery = true)
//	Number nextvalMID();
//
//	@Modifying
//	@Query(value = "ALTER SEQUENCE {h-schema}mid_sequence RESTART WITH 10000", nativeQuery = true)
//	void nextvalMID(Long lastValue);

	@Modifying
	@Query(value = "UPDATE {h-schema}log set rep_status = ?1 where uid = ?3 and mid = ?2 and type = 'PUBLISH'", nativeQuery = true)
	void updateStatus(Long status, Long mId, String uid);

	List<Log> findByMsnAndMid(String msn, Long mId);
	
	List<Log> findByUidAndMid(String uid, Long mId);
	
	@Query(value = "FROM Log where uid = ?1 and mid = ?2 and pType=?3")
	List<Log> findByUidAndMidAndPType(String uid, Long mid, String pType);
	
	@Query(value = "select raw from {h-schema}log where uid = ?1 and type='SUBSCRIBE' and p_type=?2 order by create_date desc limit 1", nativeQuery = true)
	String findRawByUidAndPType(String uid, String pType);

	@Query(value = "select * from {h-schema}log where p_type = ?1 and type='PUBLISH' and mid =?2 order by create_date desc limit 1", nativeQuery = true)
	Optional<Log> findLastPublishByMidAndPType(String type, Long oid);

}
