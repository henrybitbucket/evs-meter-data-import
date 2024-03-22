package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.DMSApplicationSite;

public interface DMSApplicationSiteRepository extends JpaRepository<DMSApplicationSite, Long> {

	Optional<DMSApplicationSite> findByAppIdAndSiteId(Long appId, Long siteId);
}
