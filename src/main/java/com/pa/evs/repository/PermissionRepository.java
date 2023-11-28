package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.Permission;
import com.pa.evs.model.RolePermission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
	
	Optional<Permission> findById(Long id);
	List<Permission> findByNameIn(Collection<String> names);
}
 