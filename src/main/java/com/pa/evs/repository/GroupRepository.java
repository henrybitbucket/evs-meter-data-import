package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.Group;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    
    Optional<Group> findById(Long id);

}
