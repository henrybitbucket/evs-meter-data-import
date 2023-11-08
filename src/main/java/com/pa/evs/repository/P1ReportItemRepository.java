package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.P1ReportItem;

@Repository
public interface P1ReportItemRepository extends JpaRepository<P1ReportItem, Long> {

}
