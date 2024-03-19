package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.DMSProjectPicUser;

public interface DMSProjectPicUserRepository extends JpaRepository<DMSProjectPicUser, Long> {

	Optional<DMSProjectPicUser> findByPicUserEmailAndProjectId(String email, Long projectId);
}
