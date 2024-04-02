package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.DeviceIEINode;
import com.pa.evs.model.Pi;

public interface DeviceIEINodeRepository extends JpaRepository<DeviceIEINode, Long> {

	
	@Query("SELECT ieiId FROM DeviceIEINode WHERE device.id = ?1")
	List<String> findIEINodesByDeviceId(Long id);

	@Modifying
	@Query("DELETE DeviceIEINode WHERE id in (SELECT id from DeviceIEINode WHERE device.id = ?1 and ieiId not in (?2))")
	void deleteNotInIEINodes(Long id, Collection<String> ieiIds);

	@Query("FROM Pi WHERE ieiId in ?1")
	List<Pi> findPiByIEIIn(List<String> ieiNodes);

	void deleteByDeviceId(Long id);

	List<DeviceIEINode> findByDeviceId(Long deviceId);
	
	List<DeviceIEINode> findByIeiIdAndDeviceIdIn(String ieiId, Collection<Long> deviceIds);
	
}
