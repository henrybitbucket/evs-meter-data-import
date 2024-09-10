package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.Role;
import com.pa.evs.model.UserRole;

public interface UserRoleRepository extends JpaRepository<UserRole, Long> {

	@Query("FROM Role WHERE name in ?1")
	List<Role> findRoleByRoleNameIn(Collection<String> names);
	
	@Query("SELECT role.name FROM UserRole WHERE user.userId = ?1")
	List<String> findRoleNameByUserId(Long userId);

	@Modifying
	@Query("DELETE UserRole WHERE id in (SELECT id from UserRole WHERE user.userId = ?1 and role.appCode.name = ?2 and role.name not in (?3))")
	void deleteNotInRoles(Long userId, String appCode, Set<String> roleNames);
	
	@Modifying
	@Query("DELETE UserRole WHERE id in (SELECT id from UserRole WHERE user.userId = ?1 and role.appCode.name = ?2 and role.id not in (?3))")
	void deleteNotInRoleIds(Long userId, String appCode, Set<Long> ids);

	List<UserRole> findByRoleId(Long id);
	
	@Query("FROM UserRole ug WHERE ug.user.userId = ?1 and ug.role.appCode.name = ?2")
	List<UserRole> findByUserIdAndAppCode(Long userId, String appCode);
}
 