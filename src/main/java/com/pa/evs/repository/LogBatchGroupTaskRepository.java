package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.LogBatchGroupTask;

@Repository
public interface LogBatchGroupTaskRepository extends JpaRepository<LogBatchGroupTask, Long> {
    
}
