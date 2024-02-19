package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.GroupUser;

public interface GroupUserRepository extends JpaRepository<GroupUser, Long> {

	@Modifying
	@Query("DELETE UserGroup WHERE id in (SELECT id from UserGroup WHERE user.userId = ?1 and groupUser.appCode.name = ?2 and groupUser.id not in (?3))")
	void deleteNotInGroups(Long userId, String appCode, Set<Long> groupIds);
	
	@Query("Select ug.groupUser FROM UserGroup ug where ug.user.userId = ?1")
	List<GroupUser> findGroupByUserUserId(Long id);
	
	Optional<GroupUser> findByName(String name);
}
 