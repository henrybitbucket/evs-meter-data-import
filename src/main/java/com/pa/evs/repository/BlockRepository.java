package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Block;
import com.pa.evs.model.Building;


@Transactional
@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {
	List<Block> findAllByBuilding (Building building);

	Optional<Block> findByBuildingIdAndName(Long buildingId, String name);
}
