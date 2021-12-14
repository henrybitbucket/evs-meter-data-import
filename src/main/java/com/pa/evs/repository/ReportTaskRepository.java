package com.pa.evs.repository;

import com.pa.evs.model.ReportTask;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;
import java.util.List;

@Transactional
@Repository
public interface ReportTaskRepository extends JpaRepository<ReportTask, Long> {
    List<ReportTask> findByReportId(Long reportId);
}
