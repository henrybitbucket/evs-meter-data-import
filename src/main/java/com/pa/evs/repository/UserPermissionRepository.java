package com.pa.evs.repository;


import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pa.evs.model.UserPermission;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
	Optional<UserPermission> findById(Long id);
}
 