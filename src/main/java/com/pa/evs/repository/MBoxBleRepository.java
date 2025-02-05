package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.BuildingUnit;
import com.pa.evs.model.MBoxBle;

@Transactional
@Repository
public interface MBoxBleRepository extends JpaRepository<MBoxBle, Long> {
	
	List<MBoxBle> findAllByBuildingUnit(BuildingUnit buildingUnit);	
}
