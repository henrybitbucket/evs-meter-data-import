package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
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

}
