package com.pa.evs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import com.pa.evs.model.Login;

@Repository
public interface LoginRepository extends JpaRepository<Login, Long> {

	Optional<Login> findByTokenIdAndUserName(String tokenId, String username);

	void deleteByTokenIdAndUserName(String tokenId, String username);

	@Transactional
	@Modifying
	@Query(value = "DELETE FROM Login where endTime < ?1")
	void deleteExpiredLogin(long now);
}
