package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.AppCode;
import com.pa.evs.model.Permission;

public interface PermissionRepository extends JpaRepository<Permission, Long> {
	
	Optional<Permission> findById(Long id);
	List<Permission> findByNameIn(Collection<String> names);
	List<Permission> findByAppCodeNameAndNameIn(String appCode, Collection<String> names);
	List<Permission> findByAppCode(AppCode appCode);
	
	@Query("Select ug.permission FROM UserPermission ug where ug.user.userId = ?1")
	List<Permission> findPermissionByUserUserId(Long id);
	
	@Query("Select ug.permission FROM UserPermission ug where ug.user.userId = ?1 and ug.permission.appCode.name = ?2")
	List<Permission> findPermissionByUserUserId(Long id, String appCode);
}
 