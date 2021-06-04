package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
	
	List<Role> findByNameIn(Collection<String> names);
}
 