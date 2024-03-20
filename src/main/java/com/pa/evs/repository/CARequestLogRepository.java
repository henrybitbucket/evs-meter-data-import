package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.enums.DeviceStatus;
import com.pa.evs.enums.DeviceType;
import com.pa.evs.model.CARequestLog;

@Transactional
@Repository
public interface CARequestLogRepository extends JpaRepository<CARequestLog, Long> {

	Optional<CARequestLog> findByUid(String uid);
	
	Optional<CARequestLog> findByBuildingUnitId(Long unitId);
	List<CARequestLog> findByMsnIn(Collection<String> msn);
	
	Optional<CARequestLog> findByMsn(String msn);
	
	@Query(value = "SELECT * FROM {h-schema}ca_request_log WHERE msn = ?1 limit 1", nativeQuery = true)
	Optional<CARequestLog> findOneByMsn(String msn);
	
	Optional<CARequestLog> findByUidAndMsn(String uid, String msn);
	
	Optional<CARequestLog> findBySn(String sn);
	
	@Query(value = "SELECT sn FROM {h-schema}ca_request_log WHERE sn in (?1)", nativeQuery = true)
	Set<String> findSnBySnIn(java.util.Collection<String> sns);
	
	@Query(value = "SELECT uid FROM {h-schema}ca_request_log WHERE uid in (?1)", nativeQuery = true)
	Set<String> findUidByUidIn(java.util.Collection<String> uids);
	
	@Query(value = "SELECT cid FROM {h-schema}ca_request_log WHERE cid in (?1)", nativeQuery = true)
	Set<String> findCidByCidIn(java.util.Collection<String> cids);
	
	Optional<CARequestLog> findByCid(String cid);
	
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

	@Transactional(propagation = Propagation.REQUIRES_NEW)
	@Modifying
	@Query(value = "update {h-schema}ca_request_log set status = 'OFFLINE' where (msn is null or msn = '') or sn is not null and (EXTRACT(EPOCH FROM (SELECT NOW())) * 1000 - COALESCE(last_subscribe_datetime, 0)) > (COALESCE(interval, 60) * 60 * 1000)", nativeQuery = true)
	void checkDevicesOffline();

	// @Query(value = "select count(id) from {h-schema}log where msn <> '' and mid is not null and topic <> 'evs/pa/local/data/send' and (rep_status = -999 or (rep_status is not null and rep_status <> 0)) and type = 'PUBLISH' and (mark_view is null or mark_view <> 1)", nativeQuery = true)
	@Query(value = "select count(id) from {h-schema}log where mid is not null and topic <> 'evs/pa/local/data/send' and (rep_status = -999 or (rep_status is not null and rep_status <> 0)) and type = 'PUBLISH' and (mark_view is null or mark_view <> 1)", nativeQuery = true)
	Number countAlarms();

	@Query("SELECT COUNT(*) FROM CARequestLog WHERE status = ?1")
    Integer getCountDevicesByStatus(DeviceStatus status);
	
	@Query("SELECT COUNT(*) FROM CARequestLog WHERE type = ?1")
    Integer getCountDevicesByType(DeviceType type);

    @Query(value = "SELECT now()", nativeQuery = true)
    void checkDatabase();

    @Query(value = "SELECT pg_database_size('pa_evs_db')", nativeQuery = true)
    Long getDatabaseSize();
    
    @Modifying
	@Query(value = "update {h-schema}log set mark_view = 1 WHERE (rep_status = -999 or (rep_status is not null and rep_status <> 0))", nativeQuery = true)
    void markViewAll();

    @Query(value = "select * from {h-schema}ca_request_log where group_id in (?1)", nativeQuery = true)
    List<CARequestLog> findDevicesInGroup(List<Long> listGroupId);
    
    @Query(value = "SELECT * from {h-schema}ca_request_log where building_id = ?1 and floor_level_id = ?2 and building_unit_id = ?3", nativeQuery = true)
    List<CARequestLog> findByBuildingAndFloorLevelAndBuildingUnit(Long buildingId, Long floorLevelId, Long buildingUnitId);
    
    @Query(value = "SELECT * from {h-schema}ca_request_log where building_id = ?1 and floor_level_id = ?2 and building_unit_id is null", nativeQuery = true)
	List<CARequestLog> findByBuildingAndFloorLevel(Long buildingId, Long floorLevelId);
    
    @Query(value = "SELECT * from {h-schema}ca_request_log where building_id = ?1 and floor_level_id is null and building_unit_id is null", nativeQuery = true)
	List<CARequestLog> findByBuilding(Long buildingId);
    
	@Modifying
	@Query(value = "update {h-schema}ca_request_log set vendor_id = ?1 where vendor_id is null", nativeQuery = true)
	void updateVendor(Long vendorId);

	List<CARequestLog> findByIdIn(List<Long> deviceIds);
}
