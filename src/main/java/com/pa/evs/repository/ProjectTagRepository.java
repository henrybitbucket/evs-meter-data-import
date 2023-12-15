package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.ProjectTag;

@Repository
public interface ProjectTagRepository extends JpaRepository<ProjectTag, Long> {

	Optional<ProjectTag> findByName(String name);

	@Query(value = "From ProjectTag where name = ?1 and id <> ?2")
	List<ProjectTag> findByNameAndDifferenceId(String name, Long id);

	List<ProjectTag> findByIdIn(List<Long> projectTags);

	List<ProjectTag> findByNameIn(List<String> tags);

}
