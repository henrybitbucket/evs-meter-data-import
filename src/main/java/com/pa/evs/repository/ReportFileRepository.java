package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.ReportFile;

@Transactional
@Repository
public interface ReportFileRepository extends JpaRepository<ReportFile, Long> {
	 List<ReportFile> findByReportTaskId(Long id);
}
