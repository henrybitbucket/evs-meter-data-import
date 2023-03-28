package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {

	@Query("FROM Address where street IN (?1)")
	List<Address> findAllByStreet(Collection<String> streets); 
}
