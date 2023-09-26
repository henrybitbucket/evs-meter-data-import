package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.MeterCommissioningReport;

@Repository
public interface MeterCommissioningReportRepository extends JpaRepository<MeterCommissioningReport, Long> {

	@Query(value = "select * from  {h-schema}meter_commissioning_report where uid = ?1 and msn = ?2 order by id desc limit 1", nativeQuery = true)
	Optional<MeterCommissioningReport> findLastSubmit(String uid, String msn);
	
	List<MeterCommissioningReport> findByJobByAndJobSheetNo(String jobBy, String jobSheetNo);

}
