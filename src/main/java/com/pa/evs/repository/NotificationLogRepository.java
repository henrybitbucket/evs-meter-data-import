package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.NotificationLog;


@Transactional
@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long>{

}
