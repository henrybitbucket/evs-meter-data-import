package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.MeterFileData;

@Repository
public interface MeterFileDataRepository extends JpaRepository<MeterFileData, Long> {
    
}
