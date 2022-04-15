package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.AppEventLog;


@Transactional
@Repository
public interface AppEventLogRepository extends JpaRepository<AppEventLog, Long>{

}
