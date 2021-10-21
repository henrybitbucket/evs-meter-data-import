package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

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
	
	Optional<CARequestLog> findBySn(String sn);
	
	@Query("SELECT certificate FROM CARequestLog WHERE uid = ?1")
	List<String> findCAByUid(String uid);
	
	@Modifying
	@Query("UPDATE CARequestLog SET msn = ?2 WHERE uid = ?1")
	void linkMsn(String uuid, String msn);
	
	@Modifying
	@Query("UPDATE CARequestLog SET msn = ?2 WHERE sn = ?1")
	void linkMsnBySn(String sn, String msn);
	
	@Query(value = "SELECT DISTINCT cid FROM {h-schema}ca_request_log", nativeQuery = true)
	List<String> getCids();
	
    @Query(value = "select exists (select * from {h-schema}ca_request_log where upper(msn) = upper(?1))", nativeQuery = true)
    Boolean existsByMsn(String msn);

	@Modifying
	@Query("UPDATE CARequestLog set activationDate = ?1 WHERE id in ?2")
	void setActivationDate(Long activationDate,Set<Long> ids);

	@Modifying
	@Query(value = "UPDATE {h-schema}ca_request_log set status = 'OFFLINE'", nativeQuery = true)
	void checkDevicesOffline();

}
