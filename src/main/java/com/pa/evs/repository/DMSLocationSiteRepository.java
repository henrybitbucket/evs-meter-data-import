package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.DMSLocationSite;


@Transactional
@Repository
public interface DMSLocationSiteRepository extends JpaRepository<DMSLocationSite, Long> {

	Optional<DMSLocationSite> findBySiteLabel(String siteLabel);
	Optional<DMSLocationSite> findBySiteId(String siteId);
	Optional<DMSLocationSite> findBySiteIdAndLocationKey(Long siteId, String locationKey);
	
}
