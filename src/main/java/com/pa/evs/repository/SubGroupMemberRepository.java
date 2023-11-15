package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.SubGroupMember;

public interface SubGroupMemberRepository extends JpaRepository<SubGroupMember, Long> {

	List<SubGroupMember> findByGroupIdAndEmailIn(Long id, List<String> members);
	
	List<SubGroupMember> findByEmail(String email);

	List<SubGroupMember> findByGroupId(Long subGroupId);

	Optional<SubGroupMember> findByGroupIdAndEmail(Long longValue, String memberEmail);

}
