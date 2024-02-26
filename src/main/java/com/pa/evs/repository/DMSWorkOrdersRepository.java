package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.DMSWorkOrders;


@Transactional
@Repository
public interface DMSWorkOrdersRepository extends JpaRepository<DMSWorkOrders, Long> {

	List<DMSWorkOrders> findBySiteLabel(String siteLabel);
	Optional<DMSWorkOrders> findBySiteId(String siteId);
	Optional<DMSWorkOrders> findByName(String name);
	Optional<DMSWorkOrders> findByGroupIdAndSiteId(Long groupId, Long siteId);
	
}
