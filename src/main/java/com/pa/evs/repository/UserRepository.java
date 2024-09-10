package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.AppCode;
import com.pa.evs.model.Permission;
import com.pa.evs.model.ProjectTag;
import com.pa.evs.model.Role;
import com.pa.evs.model.Users;

public interface UserRepository extends JpaRepository<Users, Long> {

	Users findByEmail(String email);
	
	List<Users> findByEmailIn(Collection<String> emails);
	
	@Query(value = "FROM Users where phoneNumber = ?1 or phoneNumber = ('+' || ?1)")
	Users findByPhoneNumber(String phoneNumber);
	
	@Query(value = "FROM Users where lcPhoneNumber = ?1 or ('0' || lcPhoneNumber) = ?1 or lcPhoneNumber = ('0' || ?1)")
	List<Users> findByLcPhoneNumber(String lcPhoneNumber);
	
	List<Users> findByPhoneNumberIn(Collection<String> phoneNumbers);
	
	Users findByUsername(String username);
	
	List<Users> findByUserIdIn(Collection<Long> userIds);
	
	Optional<Users> findById(Long id);

	@Query(value = "SELECT ur.role FROM UserRole ur where ur.user.userId = ?1")
	List<Role> findRolesByUserId(Long userId);

	@Query(value = "SELECT ur.permission FROM UserPermission ur where ur.user.userId = ?1")
	List<Permission> findPermissionsByUserId(Long userId);

	@Query(value = "SELECT ur.project FROM UserProject ur where ur.user.userId = ?1")
	List<ProjectTag> findProjectsByUserId(Long userId);

	@Query(value = "SELECT ur.appCode FROM UserAppCode ur where ur.user.userId = ?1")
	List<AppCode> findAppCodesByUserId(Long userId);

}
