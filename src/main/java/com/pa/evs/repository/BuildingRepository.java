package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Building;


@Transactional
@Repository
public interface BuildingRepository extends JpaRepository<Building, Long> {


}
