package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.Firmware;

public interface FirmwareRepository extends JpaRepository<Firmware, Long>{

}
