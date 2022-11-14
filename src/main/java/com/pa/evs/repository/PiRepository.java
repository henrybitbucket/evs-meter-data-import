package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.Pi;

@Repository
public interface PiRepository extends JpaRepository<Pi, Long> {
    
	Optional<Pi> findByUuid(String uuid);
	
	@Query("FROM Pi WHERE email is not null and (hide is null or hide = false)")
	List<Pi> findExists();

	Optional<Pi> findByIeiId(String ieiId);
}
