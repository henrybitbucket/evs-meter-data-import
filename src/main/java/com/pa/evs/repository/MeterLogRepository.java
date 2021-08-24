package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.MeterLog;

import java.util.List;

@Transactional
public interface MeterLogRepository extends JpaRepository<MeterLog, Long> {

    @Query(value = "SELECT * FROM {h-schema}MeterLog WHERE uid = ?1 AND dt >= ?2 and dt <= ?3", nativeQuery = true)
    List<MeterLog> getMeterList(final String uid, final Long p1, final Long p2);

}
