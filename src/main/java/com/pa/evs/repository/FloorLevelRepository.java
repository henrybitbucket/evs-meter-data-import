package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Block;
import com.pa.evs.model.Building;
import com.pa.evs.model.FloorLevel;


@Transactional
@Repository
public interface FloorLevelRepository extends JpaRepository<FloorLevel, Long> {
	List<FloorLevel> findAllByBlock (Block block);
	
	@Query("FROM FloorLevel where block is null")
	List<FloorLevel> findAllByBlockIsNull();
	
	List<FloorLevel> findAllByBuilding (Building building);
	
	Optional<FloorLevel> findByBuildingIdAndName(Long buildingId, String name);
	
	Optional<FloorLevel> findByBuildingIdAndBlockIdAndName(Long buildingId, Long blockId, String name);
	
	@Query(value = "select sn from CARequestLog where floorLevel.id = ?1")
	List<String> linkedSN(long floorLevelId);
}
