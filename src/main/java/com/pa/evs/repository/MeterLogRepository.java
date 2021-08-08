package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.MeterLog;

@Transactional
public interface MeterLogRepository extends JpaRepository<MeterLog, Long> {
	
}
