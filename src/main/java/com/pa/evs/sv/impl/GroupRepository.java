package com.pa.evs.sv.impl;

import com.pa.evs.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Transactional
public interface GroupRepository extends JpaRepository<Group, Long> {
    Optional<Group> findById(String id);
}
