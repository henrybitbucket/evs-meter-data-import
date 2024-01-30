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
	
	@Query("FROM Building bd where upper(bd.name || '__' || bd.address.city) in (?1) ")
	List<Building> findAllByBuingNameAndCity(Set<String> buildingCity);
	
	@Query("FROM Building bd where upper(bd.address.postalCode || '__' || bd.address.city || '__' || bd.name) in (?1) ")
	List<Building> findAllByPostalCodeAndCityAndName(Set<String> cityPostalCodeName);

	@Query("FROM Building bd where bd.name in (?1) ")
	List<Building> findAllByName(Set<String> names);
	
	@Query(value = "select sn from CARequestLog where building.id = ?1")
	List<String> linkedSN(long buildingId);
	
	@Query(value = "select msn from MMSMeter where building.id = ?1")
	List<String> linkedMSN(long buildingId);
}
