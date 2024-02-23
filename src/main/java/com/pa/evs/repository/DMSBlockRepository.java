package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.DMSBuilding;
import com.pa.evs.model.DMSBlock;


@Transactional
@Repository
public interface DMSBlockRepository extends JpaRepository<DMSBlock, Long> {
	List<DMSBlock> findAllByBuilding(DMSBuilding building);

	Optional<DMSBlock> findByBuildingIdAndName(Long buildingId, String name);
}
