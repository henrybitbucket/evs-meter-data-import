package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.GroupUser;
import com.pa.evs.model.Role;
import com.pa.evs.model.RoleGroup;
import com.pa.evs.model.Users;

public interface GroupUserRepository extends JpaRepository<GroupUser, Long> {

	@Modifying
	@Query("DELETE UserGroup WHERE id in (SELECT id from UserGroup WHERE user.userId = ?1 and groupUser.appCode.name = ?2 and groupUser.id not in (?3))")
	void deleteNotInGroups(Long userId, String appCode, Set<Long> groupIds);
	
	@Query("Select ug.groupUser FROM UserGroup ug where ug.user.userId = ?1")
	List<GroupUser> findGroupByUserUserId(Long id);
	
	Optional<GroupUser> findByName(String name);
	
	List<GroupUser> findByAppCodeName(String name);
	
	@Query("Select ug.user FROM UserGroup ug where ug.user.email in (?1) and ug.groupUser.name = ?2 and ug.groupUser.appCode.name = ?3")
	List<Users> findUserByEmailAndGroupName(Collection<String> emails, String groupName, String appCodeName);

	@Query("SELECT ps FROM RoleGroup ps where ps.groupUser.id = ?1 ")
	List<RoleGroup> findRolesInGroup(Long groupUserId);

	@Query("SELECT role FROM Role role where role.id in ?1 ")
	List<Role> findRolesIn(List<Long> roleIds);
}
 