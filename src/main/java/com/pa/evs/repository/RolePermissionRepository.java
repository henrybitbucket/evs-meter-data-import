package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.RolePermission;

public interface RolePermissionRepository extends JpaRepository<RolePermission, Long> {
	Optional<RolePermission> findById(Long id);
	List<RolePermission> findByRoleId(Long roleId);
	
	List<RolePermission> findByRoleNameIn(Collection<String> roleNames);
}
 