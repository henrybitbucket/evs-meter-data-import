package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.P2ReportAck;

@Repository
public interface P2ReportAckRepository extends JpaRepository<P2ReportAck, Long> {

}
