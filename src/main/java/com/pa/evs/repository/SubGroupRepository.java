package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.SubGroup;

public interface SubGroupRepository extends JpaRepository<SubGroup, Long> {

	Optional<SubGroup> findByName(String name);
	
	Optional<SubGroup> findByNameAndOwner(String name, String owner);
	
	List<SubGroup> findByOwner(String owner);

	Optional<SubGroup> findByIdAndOwner(Long id, String email);

}
