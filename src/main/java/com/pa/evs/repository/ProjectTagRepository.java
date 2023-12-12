package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.ProjectTag;

@Repository
public interface ProjectTagRepository extends JpaRepository<ProjectTag, Long> {

	Optional<ProjectTag> findByName(String name);

}
