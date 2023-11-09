package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.GroupUser;
import com.pa.evs.model.SubGroup;

public interface SubGroupRepository extends JpaRepository<SubGroup, Long> {

	Optional<SubGroup> findByName(String name);
	
	Optional<SubGroup> findByNameAndOwner(String name, String owner);

}
