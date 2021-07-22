package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.CARequestLog;

@Transactional
public interface CARequestLogRepository extends JpaRepository<CARequestLog, Long> {

	Optional<CARequestLog> findByUid(String uid);
	Optional<CARequestLog> findByMsn(String msn);
	
	@Query(value = "SELECT * FROM ca_request_log WHERE msn = ?1 limit 1", nativeQuery = true)
	Optional<CARequestLog> findOneByMsn(String msn);
	
	Optional<CARequestLog> findByUidAndMsn(String uid, String msn);
	
	@Query("SELECT certificate FROM CARequestLog WHERE uid = ?1")
	List<String> findCAByUid(String uid);
	
	@Modifying
	@Query("UPDATE CARequestLog SET msn = ?2 WHERE uid = ?1")
	void linkMsn(String uuid, String msn);
	
	@Modifying
	@Query("UPDATE CARequestLog SET msn = ?2 WHERE sn = ?1")
	void linkMsnBySn(String sn, String msn);
}
