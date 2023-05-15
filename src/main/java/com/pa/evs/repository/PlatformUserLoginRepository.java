package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.PlatformUserLogin;

public interface PlatformUserLoginRepository extends JpaRepository<PlatformUserLogin, Long> {

	PlatformUserLogin findByEmailAndName(String email, String name);
	List<PlatformUserLogin> findByEmail(String email);
}
