package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import com.pa.evs.model.ProjectTag;
import com.pa.evs.model.UserProject;

public interface UserProjectRepository extends JpaRepository<UserProject, Long> {

	@Query("FROM ProjectTag WHERE name in ?1")
	List<ProjectTag> findProjectByProjectTagNameIn(Collection<String> names);
	
	@Query("SELECT project.name FROM UserProject WHERE user.userId = ?1")
	List<String> findProjectNameByUserId(Long userId);

	@Modifying
	@Query("DELETE UserProject WHERE id in (SELECT id from UserProject WHERE user.userId = ?1 and project.name not in (?2))")
	void deleteNotInProjects(Long userId, Set<String> projectNames);
}
 