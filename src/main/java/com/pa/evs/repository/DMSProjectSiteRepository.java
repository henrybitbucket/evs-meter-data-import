package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.DMSProjectSite;

public interface DMSProjectSiteRepository extends JpaRepository<DMSProjectSite, Long> {

	List<DMSProjectSite> findByProjectId(Long id);

	Optional<DMSProjectSite> findByProjectIdAndSiteId(Long projectId, Long siteId);

}
