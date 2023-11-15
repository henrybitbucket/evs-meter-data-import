package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.SubGroupMemberRole;

public interface SubGroupMemberRoleRepository extends JpaRepository<SubGroupMemberRole, Long> {

	List<SubGroupMemberRole> findByMemberIdAndRoleIn(Long id, List<String> roles);

	List<SubGroupMemberRole> findByMemberId(Long id);

	List<SubGroupMemberRole> findByMemberIdIn(Collection<Long> memberIds);

}
