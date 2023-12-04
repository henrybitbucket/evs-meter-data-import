package com.pa.evs.repository;


import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.UserGroup;

public interface UserGroupRepository extends JpaRepository<UserGroup, Long> {

	List<UserGroup> findByUserUserIdIn(List<Long> asList);

}
 