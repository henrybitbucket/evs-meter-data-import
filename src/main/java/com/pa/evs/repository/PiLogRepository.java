package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.PiLog;

@Repository
public interface PiLogRepository extends JpaRepository<PiLog, Long> {
    
	List<PiLog> findByMsnAndMid(String msn, Long mid);
	
	List<PiLog> findByMsnAndMidAndPiId(String msn, Long mid, Long piId);
	
	List<PiLog> findByMsnAndMidAndPiUuid(String msn, Long mid, String piUuid);

	List<PiLog> findByMsnAndMidAndPiIeiId(String msn, Long mid, String ieiId);
	
	List<PiLog> findByMsnAndMidAndPiIeiIdAndLogId(String msn, Long mid, String ieiId, Long logId);
	
	@Query("FROM PiLog where ftpResStatus = 'NEW' AND fileName is null")
	List<PiLog> findByFileNameIsNull();
}
