package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.P1OnlineStatus;

@Repository
public interface P1OnlineStatusRepository extends JpaRepository<P1OnlineStatus, Long> {

	Optional<P1OnlineStatus> findByUid(String uid);

	@Query(value = "from P1OnlineStatus where uid = ?1 and isLatest = true")
	Optional<P1OnlineStatus> findLatestByUid(String uid);

}
