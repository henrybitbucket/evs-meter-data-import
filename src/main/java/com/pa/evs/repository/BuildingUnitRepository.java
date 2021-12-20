package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Building;
import com.pa.evs.model.BuildingUnit;


@Transactional
@Repository
public interface BuildingUnitRepository extends JpaRepository<BuildingUnit, Long>{

	void save(Building buildingUnit);

}
