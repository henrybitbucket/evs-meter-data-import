package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.DMSLocationLock;


@Transactional
@Repository
public interface DMSLocationLockRepository extends JpaRepository<DMSLocationLock, Long> {

	Optional<DMSLocationLock> findByLockId(Long lockId);
	Optional<DMSLocationLock> findByLockIdAndLocationKey(Long lockId, String locationKey);
	
	@Query(value = "select s.lock_number from {h-schema}dms_lock s where exists (select 1 from {h-schema}dms_location_lock ls where ls.building_id = ?1 and ls.lock_id = s.id) limit 1", nativeQuery = true)
	List<String> findLockByBuildingId(Long buildingId);
	
	@Query(value = "select s.lock_number from {h-schema}dms_lock s where exists (select 1 from {h-schema}dms_location_lock ls where ls.block_id = ?1 and ls.lock_id = s.id) limit 1", nativeQuery = true)
	List<String> findLockByBlockId(Long BlockId);
	
	@Query(value = "select s.lock_number from {h-schema}dms_lock s where exists (select 1 from {h-schema}dms_location_lock ls where ls.floor_level_id = ?1 and ls.lock_id = s.id) limit 1", nativeQuery = true)
	List<String> findLockByFloorLevelId(Long buildingId);
	
	@Query(value = "select s.lock_number from {h-schema}dms_lock s where exists (select 1 from {h-schema}dms_location_lock ls where ls.building_unit_id = ?1 and ls.lock_id = s.id) limit 1", nativeQuery = true)
	List<String> findLockByBuildingUnitId(Long buildingId);
	
}
