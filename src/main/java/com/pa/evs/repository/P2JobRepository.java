package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.P2Job;

@Repository
public interface P2JobRepository extends JpaRepository<P2Job, Long> {

	Optional<P2Job> findByJobByAndName(String jobBy, String name);

	void deleteByJobByAndName(String user, String jobNo);
	
	@Query(value = "select * from {h-schema}p2_job where job_by = ?1 order by name desc ", nativeQuery = true)
	List<P2Job> findAllByJobBy(String jobBy);
	
	@Query(value = "select job.* from {h-schema}p2_job job where job_by = ?1 and exists (select 1 from {h-schema}meter_commissioning_report rp where rp.job_by = job.job_by and rp.job_sheet_no = job.name) order by name desc ", nativeQuery = true)
	List<P2Job> findAllByJobByAndHasReport(String jobBy);

}
