package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.Role;
import com.pa.evs.model.RoleGroup;

public interface RoleGroupRepository extends JpaRepository<RoleGroup, Long> {
	Optional<RoleGroup> findById(Long id);
	List<RoleGroup> findByRoleId(Long role_id);
	List<RoleGroup> findByGroupUserNameIn(Collection<String> groupNames);
	
	@Query(value = "SELECT rg.role FROM RoleGroup rg where rg.groupUser.name in (?1)")
	List<Role> findRoleByGroupUserNameIn(Collection<String> groupNames);

	List<RoleGroup> findByGroupUserId(Long groupUserId);
}
 