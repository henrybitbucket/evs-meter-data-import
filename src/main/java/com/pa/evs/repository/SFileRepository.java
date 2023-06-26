package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.SFile;

@Repository
public interface SFileRepository extends JpaRepository<SFile, Long> {

}
