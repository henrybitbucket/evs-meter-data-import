package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.SFile;

@Repository
public interface SFileRepository extends JpaRepository<SFile, Long> {

	void deleteByTypeAndOriginalName(String type, String originalFilename);

	Optional<SFile> findByTypeAndAltName(String string, String altName);

	void deleteByTypeAndAltName(String string, String altName);

}
