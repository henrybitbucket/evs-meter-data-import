package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.DMSApplicationUser;

public interface DMSApplicationUserRepository extends JpaRepository<DMSApplicationUser, Long> {

}
