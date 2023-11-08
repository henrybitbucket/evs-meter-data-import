package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.P1Report;

@Repository
public interface P1ReportRepository extends JpaRepository<P1Report, Long> {

}
