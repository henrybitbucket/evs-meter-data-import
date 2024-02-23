package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.DMSAddress;

public interface DMSAddressRepository extends JpaRepository<DMSAddress, Long> {

	@Query("FROM DMSAddress where street IN (?1)")
	List<DMSAddress> findAllByStreet(Collection<String> streets); 
}
