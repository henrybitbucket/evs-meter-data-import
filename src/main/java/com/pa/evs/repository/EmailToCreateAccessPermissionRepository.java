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

	@Query(value = "SELECT * FROM {h-schema}email_create_access_permission e WHERE (retry IS NULL OR retry < :retryTimes) AND is_processed = :isProcessed limit 1", nativeQuery = true)
	List<EmailToCreateAccessPermission> findByRetryIsNullOrRetryLessThanAndIsProcessed(@Param("retryTimes") int retryTimes, @Param("isProcessed") boolean isProcessed);

}
