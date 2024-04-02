package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.DMSApplication;

public interface DMSApplicationRepository extends JpaRepository<DMSApplication, Long> {

	List<DMSApplication> findByProjectId(Long projectId);
}
