package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.Users;

public interface UserRepository extends JpaRepository<Users, Long> {

	Users findByEmail(String email);
	
	List<Users> findByEmailIn(Collection<String> emails);
	
	Users findByPhoneNumber(String phoneNumber);
	
	List<Users> findByPhoneNumberIn(Collection<String> phoneNumbers);
	
	Users findByUsername(String username);
	
	List<Users> findByUserIdIn(Collection<Long> userIds);
	
	Optional<Users> findById(Long id);
}
