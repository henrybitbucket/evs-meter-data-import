package com.pa.evs.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.DMSProjectSite;

public interface DMSProjectSiteRepository extends JpaRepository<DMSProjectSite, Long> {

	List<DMSProjectSite> findByProjectId(Long id);

}
