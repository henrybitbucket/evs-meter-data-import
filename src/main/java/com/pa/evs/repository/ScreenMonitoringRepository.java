package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.enums.ScreenMonitorKey;
import com.pa.evs.model.ScreenMonitoring;

@Repository
public interface ScreenMonitoringRepository extends JpaRepository<ScreenMonitoring, Long> {

    ScreenMonitoring findFirstByOrderByIdDesc();

    Optional<ScreenMonitoring> findByKey(ScreenMonitorKey dbCheck);
}