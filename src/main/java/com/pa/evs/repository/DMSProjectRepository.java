package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.DMSProject;
import com.pa.evs.model.DMSProjectSite;
import com.pa.evs.model.DMSSite;

public interface DMSProjectRepository extends JpaRepository<DMSProject, Long> {

	Optional<DMSProject> findByName(String name);
	
	Optional<DMSProject> findByDisplayName(String displayName);
	
	@Query("SELECT ps FROM DMSProjectSite ps where ps.project.id = ?1 ")
	List<DMSProjectSite> findSitesInProject(Long dmsProjectId);
	
	@Query("SELECT site FROM DMSSite site where site.id in ?1 ")
	List<DMSSite> findSitesIn(Collection<Long> siteIds);
}
