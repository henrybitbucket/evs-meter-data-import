package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.EmailToCreateAccessPermission;

@Repository
public interface EmailToCreateAccessPermissionRepository extends JpaRepository<EmailToCreateAccessPermission, Long> {

	Optional<EmailToCreateAccessPermission> findByMessageIdAndIsProcessed(String messageId, boolean isProcessed);

	List<EmailToCreateAccessPermission> findByIsProcessed(boolean isProcessed);

	Optional<EmailToCreateAccessPermission> findByMessageId(String messageId);

	@Query("SELECT e FROM EmailToCreateAccessPermission e WHERE (e.retry IS NULL OR e.retry < :retryTimes) AND e.isProcessed = :isProcessed")
	List<EmailToCreateAccessPermission> findByRetryIsNullOrRetryLessThanAndIsProcessed(@Param("retryTimes") int retryTimes, @Param("isProcessed") boolean isProcessed);

}
