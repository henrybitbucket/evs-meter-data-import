package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.DMSLocationSite;


@Transactional
@Repository
public interface DMSLocationSiteRepository extends JpaRepository<DMSLocationSite, Long> {

	Optional<DMSLocationSite> findBySiteLabel(String siteLabel);
	Optional<DMSLocationSite> findBySiteId(String siteId);
	Optional<DMSLocationSite> findBySiteIdAndLocationKey(Long siteId, String locationKey);
	
	
	@Query(value = "select s.label from {h-schema}dms_site s where exists (select 1 from {h-schema}dms_location_site ls where ls.building_id = ?1 and ls.site_id = s.id) limit 1", nativeQuery = true)
	List<String> findSiteByBuildingId(Long buildingId);
	
	@Query(value = "select s.label from {h-schema}dms_site s where exists (select 1 from {h-schema}dms_location_site ls where ls.block_id = ?1 and ls.site_id = s.id) limit 1", nativeQuery = true)
	List<String> findSiteByBlockId(Long BlockId);
	
	@Query(value = "select s.label from {h-schema}dms_site s where exists (select 1 from {h-schema}dms_location_site ls where ls.floor_level_id = ?1 and ls.site_id = s.id) limit 1", nativeQuery = true)
	List<String> findSiteByFloorLevelId(Long buildingId);
	
	@Query(value = "select s.label from {h-schema}dms_site s where exists (select 1 from {h-schema}dms_location_site ls where ls.building_unit_id = ?1 and ls.site_id = s.id) limit 1", nativeQuery = true)
	List<String> findSiteByBuildingUnitId(Long buildingId);
}
