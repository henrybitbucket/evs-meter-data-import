package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.ReportTask;

@Transactional
@Repository
public interface ReportTaskRepository extends JpaRepository<ReportTask, Long> {
    List<ReportTask> findByReportId(Long reportId);
}
