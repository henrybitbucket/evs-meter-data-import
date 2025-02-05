package com.pa.evs.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.GroupUser;
import com.pa.evs.model.UserGroup;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {


	@Query(value = "SELECT ug.groupUser FROM UserGroup ug where ug.user.userId in ?1")
	List<GroupUser> findGroupUserByUserIdIn(List<Long> asList);

	List<UserGroup> findByUserUserIdIn(List<Long> asList);

	List<UserGroup> findByGroupUserId(Long id);

	@Query("FROM UserGroup ug WHERE ug.user.userId = ?1 and ug.groupUser.appCode.name = ?2")
	List<UserGroup> findByUserIdAndAppCode(Long userId, String appCode);
	
}
 