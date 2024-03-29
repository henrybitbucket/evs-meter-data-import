package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.DMSApplicationUser;

public interface DMSApplicationUserRepository extends JpaRepository<DMSApplicationUser, Long> {

	List<DMSApplicationUser> findByAppId(Long applicationId);
}
