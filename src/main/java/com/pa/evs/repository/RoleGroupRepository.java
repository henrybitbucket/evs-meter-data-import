package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.pa.evs.model.RoleGroup;

public interface RoleGroupRepository extends JpaRepository<RoleGroup, Long> {
	Optional<RoleGroup> findById(Long id);
	List<RoleGroup> findByRoleId(Long role_id);
}
 