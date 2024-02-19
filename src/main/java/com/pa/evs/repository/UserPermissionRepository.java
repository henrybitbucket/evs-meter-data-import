package com.pa.evs.repository;


import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.Permission;
import com.pa.evs.model.UserPermission;

public interface UserPermissionRepository extends JpaRepository<UserPermission, Long> {
	Optional<UserPermission> findById(Long id);

	List<UserPermission> findByUserUserId(Long id);
	
	@Query("Select up.permission FROM UserPermission up where up.permission.appCode.name = ?1 and up.user.userId = ?2")
	List<Permission> findPermissionByAppCodeNameAndUserUserId(String appCode, Long id);
	
	@Modifying
	@Query("DELETE FROM UserPermission up where up.user.userId = ?1 AND up.permission.id = ?2")
	void deleteByUserIdAndPermissionId(Long userId, Long pId);
}
