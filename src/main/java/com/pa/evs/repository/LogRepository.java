package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Log;

@Transactional
public interface LogRepository extends JpaRepository<Log, Long> {
	
}
