package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Building;


@Transactional
@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {

	Optional<Building> findByAddressId(Long addressId);
	@Query("FROM Building bd where bd.address.street in (?1) ")
	List<Building> findAllByAddressStreet(Set<String> streets);
}
