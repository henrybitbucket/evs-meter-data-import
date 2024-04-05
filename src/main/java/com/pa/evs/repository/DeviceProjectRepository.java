package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.DeviceProject;
import com.pa.evs.model.ProjectTag;

public interface DeviceProjectRepository extends JpaRepository<DeviceProject, Long> {

	@Query("FROM ProjectTag WHERE id in ?1")
	List<ProjectTag> findProjectByProjectTagIdIn(Collection<Long> ids);
	
	@Query("SELECT project.name FROM DeviceProject WHERE device.id = ?1 and type = ?2")
	List<String> findProjectNameByDeviceId(Long id, String type);

	@Modifying
	@Query("DELETE DeviceProject WHERE id in (SELECT id from DeviceProject WHERE device.id = ?1 and project.id not in (?2)) and type = ?3")
	void deleteNotInProjects(Long id, Set<Long> projectIds, String type);
	
}
