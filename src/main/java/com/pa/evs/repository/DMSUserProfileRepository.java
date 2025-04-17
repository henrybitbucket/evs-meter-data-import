package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.DMSUserProfile;


@Transactional
@Repository
public interface DMSUserProfileRepository extends JpaRepository<DMSUserProfile, Long> {
	DMSUserProfile findByEmail(String email);
}
