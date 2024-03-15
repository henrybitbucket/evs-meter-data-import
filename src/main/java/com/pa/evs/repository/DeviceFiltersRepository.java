package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.DeviceFilters;
import com.pa.evs.model.Users;

@Repository
public interface DeviceFiltersRepository extends JpaRepository<DeviceFilters, Long> {

	Optional<DeviceFilters> findByName(String name);

	List<DeviceFilters> findByUser(Users loggedInUser);

	Optional<DeviceFilters> findByNameAndUser(String name, Users loggedInUser);

}
