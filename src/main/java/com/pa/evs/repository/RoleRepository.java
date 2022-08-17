package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.Role;

public interface RoleRepository extends JpaRepository<Role, Long> {
	
	List<Role> findByNameIn(Collection<String> names);
	Optional<Role> findById(Long id);
	
	@Query("Select ur.role FROM UserRole ur where ur.user.userId = ?1")
	List<Role> findRoleByUserUserId(Long id);
}
 