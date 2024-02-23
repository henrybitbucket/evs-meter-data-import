package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.DMSSite;

public interface DMSSiteRepository extends JpaRepository<DMSSite, Long> {

	Optional<DMSSite> findByLabel(String label);
}
