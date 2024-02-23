package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.DMSBuilding;
import com.pa.evs.model.DMSBuildingUnit;
import com.pa.evs.model.DMSFloorLevel;


@Transactional
@Repository
public interface DMSBuildingUnitRepository extends JpaRepository<DMSBuildingUnit, Long>{

	List<DMSBuildingUnit> findAllByFloorLevel (DMSFloorLevel floorLevel);
	void save(DMSBuilding buildingUnit);
	
	Optional<DMSBuildingUnit> findByFloorLevelIdAndName(Long fId, String name);
}
