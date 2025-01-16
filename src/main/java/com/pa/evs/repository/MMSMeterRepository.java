package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.MMSMeter;

@Repository
public interface MMSMeterRepository extends JpaRepository<MMSMeter, Long> {

	MMSMeter findByMsn(String msn);
	
	MMSMeter findByUid(String uid);
	
	List<MMSMeter> findByMsnIn(Collection<String> msns);
	
	MMSMeter findByBuildingUnitId(Long unitId);
	
    @Query(value = "SELECT * from {h-schema}mms_meter where building_id = ?1 and floor_level_id = ?2 and building_unit_id = ?3", nativeQuery = true)
    List<MMSMeter> findByBuildingAndFloorLevelAndBuildingUnit(Long buildingId, Long floorLevelId, Long buildingUnitId);
    
    @Query(value = "SELECT * from {h-schema}mms_meter where building_id = ?1 and floor_level_id = ?2 and building_unit_id is null", nativeQuery = true)
	List<MMSMeter> findByBuildingAndFloorLevel(Long buildingId, Long floorLevelId);
    
    @Query(value = "SELECT * from {h-schema}mms_meter where building_id = ?1 and floor_level_id is null and building_unit_id is null", nativeQuery = true)
	List<MMSMeter> findByBuilding(Long buildingId);
}
