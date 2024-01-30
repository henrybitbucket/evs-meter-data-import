package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Block;
import com.pa.evs.model.Building;


@Transactional
@Repository
public interface BlockRepository extends JpaRepository<Block, Long> {
	List<Block> findAllByBuilding (Building building);

	Optional<Block> findByBuildingIdAndName(Long buildingId, String name);
	
	@Query(value = "select sn from CARequestLog where block.id = ?1")
	List<String> linkedSN(long blockId);
	
	@Query(value = "select msn from MMSMeter where block.id = ?1")
	List<String> linkedMSN(long blockId);
}
