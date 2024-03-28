package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.DMSApplicationSite;

public interface DMSApplicationSiteRepository extends JpaRepository<DMSApplicationSite, Long> {

	Optional<DMSApplicationSite> findByAppIdAndSiteId(Long appId, Long siteId);
	
	List<DMSApplicationSite> findByAppId(Long appId);
}
