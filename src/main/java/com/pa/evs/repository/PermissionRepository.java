package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
	
	Optional<Permission> findById(Long id);
}
 