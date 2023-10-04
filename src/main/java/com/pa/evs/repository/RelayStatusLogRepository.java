package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.RelayStatusLog;

public interface RelayStatusLogRepository extends JpaRepository<RelayStatusLog, Long> {

}
