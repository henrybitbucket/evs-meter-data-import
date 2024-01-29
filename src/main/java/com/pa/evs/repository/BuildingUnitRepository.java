package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Building;
import com.pa.evs.model.BuildingUnit;
import com.pa.evs.model.FloorLevel;


@Transactional
@Repository
public interface BuildingUnitRepository extends JpaRepository<BuildingUnit, Long>{

	List<BuildingUnit> findAllByFloorLevel (FloorLevel floorLevel);
	void save(Building buildingUnit);
	
	Optional<BuildingUnit> findByFloorLevelIdAndName(Long fId, String name);

	
	@Query(value = "select sn from CARequestLog where buildingUnit.id = ?1")
	List<String> linkedSN(long buildingUnitId);
}
