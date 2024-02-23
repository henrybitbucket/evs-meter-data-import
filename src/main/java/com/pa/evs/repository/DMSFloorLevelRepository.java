package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.DMSBlock;
import com.pa.evs.model.DMSBuilding;
import com.pa.evs.model.DMSFloorLevel;


@Transactional
@Repository
public interface DMSFloorLevelRepository extends JpaRepository<DMSFloorLevel, Long> {
	List<DMSFloorLevel> findAllByBlock (DMSBlock block);
	
	@Query("FROM DMSFloorLevel where block is null")
	List<DMSFloorLevel> findAllByBlockIsNull();
	
	List<DMSFloorLevel> findAllByBuilding (DMSBuilding building);
	
	Optional<DMSFloorLevel> findByBuildingIdAndName(Long buildingId, String name);
	
	Optional<DMSFloorLevel> findByBuildingIdAndBlockIdAndName(Long buildingId, Long blockId, String name);
}
