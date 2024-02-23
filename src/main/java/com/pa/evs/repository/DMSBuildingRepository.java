package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.DMSBuilding;


@Transactional
@Repository
public interface DMSBuildingRepository extends JpaRepository<DMSBuilding, Long> {

	Optional<DMSBuilding> findByAddressId(Long addressId);
	@Query("FROM DMSBuilding bd where bd.address.street in (?1) ")
	List<DMSBuilding> findAllByAddressStreet(Set<String> streets);
	
	@Query("FROM DMSBuilding bd where upper(bd.name || '__' || bd.address.city) in (?1) ")
	List<DMSBuilding> findAllByBuingNameAndCity(Set<String> buildingCity);
	
	@Query("FROM DMSBuilding bd where upper(bd.address.postalCode || '__' || bd.address.city || '__' || bd.name) in (?1) ")
	List<DMSBuilding> findAllByPostalCodeAndCityAndName(Set<String> cityPostalCodeName);

	@Query("FROM DMSBuilding bd where bd.name in (?1) ")
	List<DMSBuilding> findAllByName(Set<String> names);
}
