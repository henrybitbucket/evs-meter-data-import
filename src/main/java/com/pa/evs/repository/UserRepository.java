package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.Users;

public interface UserRepository extends JpaRepository<Users, Long> {

	Users findByEmail(String email);
	
	Users findByUsername(String username);
	
	List<Users> findByUserIdIn(Collection<Long> userIds);
	
	Optional<Users> findById(Long id);
}
