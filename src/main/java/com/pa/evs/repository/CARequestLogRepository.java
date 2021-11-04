package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.enums.DeviceStatus;
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
	@Query(value = "update {h-schema}ca_request_log set activation_date = ?1 WHERE id in (?2)", nativeQuery = true)
	void setActivationDate(Long activationDate, Set<Long> ids);

	@Modifying
	@Query(value = "update {h-schema}ca_request_log set status = 'OFFLINE' where msn is not null and sn is not null and (EXTRACT(EPOCH FROM (SELECT NOW())) * 1000 - COALESCE(last_subscribe_datetime, 0)) > (COALESCE(interval, 60) * 60 * 1000)", nativeQuery = true)
	void checkDevicesOffline();

	@Query(value = "select count(id) from {h-schema}log where mid is not null and topic <> 'evs/pa/local/data/send' and (rep_status = -999 or (rep_status is not null and rep_status <> 0)) and type = 'PUBLISH' and (mark_view is null or mark_view <> 1)", nativeQuery = true)
	Number countAlarms();

	@Query("SELECT COUNT(*) FROM CARequestLog WHERE status = ?1")
    Integer getCountDevicesByStatus(DeviceStatus status);

    @Query(value = "SELECT now()", nativeQuery = true)
    void checkDatabase();

    @Query(value = "SELECT pg_database_size('pa_evs_db')", nativeQuery = true)
    Long getDatabaseSize();
    
    @Modifying
	@Query(value = "update {h-schema}log set mark_view = 1 WHERE rep_status = -999", nativeQuery = true)
    void markViewAll();

}
