package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.Pi;

@Repository
public interface PiRepository extends JpaRepository<Pi, Long> {
    
	Optional<Pi> findByUuid(String uuid);
}
