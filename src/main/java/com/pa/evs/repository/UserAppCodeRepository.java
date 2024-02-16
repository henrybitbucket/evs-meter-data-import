package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.UserAppCode;

@Repository
public interface UserAppCodeRepository extends JpaRepository<UserAppCode, Long> {

	@Modifying
	@Query("DELETE UserAppCode WHERE id in (SELECT id from UserAppCode WHERE user.userId = ?1 and appCode.name not in (?2))")
	void deleteNotInAppCodes(Long userId, Collection<String> appCodes);
	
	@Query("SELECT appCode.name FROM UserAppCode WHERE user.userId = ?1")
	List<String> findAppCodeNameByUserUserId(Long userId);
}
