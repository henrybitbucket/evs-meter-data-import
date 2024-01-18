package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.RelayStatusLog;

public interface RelayStatusLogRepository extends JpaRepository<RelayStatusLog, Long> {

	Optional<RelayStatusLog> findByCommandAndUidAndMid(String pType, String uid, Long oid);

}
