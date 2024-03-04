package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.UserAppCode;
import com.pa.evs.model.Users;

@Repository
public interface UserAppCodeRepository extends JpaRepository<UserAppCode, Long> {

	@Modifying
	@Query("DELETE UserAppCode WHERE id in (SELECT id from UserAppCode WHERE user.userId = ?1 and appCode.name not in (?2))")
	void deleteNotInAppCodes(Long userId, Collection<String> appCodes);
	
	@Query("SELECT appCode.name FROM UserAppCode WHERE user.userId = ?1")
	List<String> findAppCodeNameByUserUserId(Long userId);
	
	@Query("SELECT ac.user FROM UserAppCode ac WHERE ac.appCode.name = ?1 and ac.user.email in (?2)")
	List<Users> findByAppCodeNameAndUserEmailIn(String appCode, Collection<String> emails);
	
	@Query("SELECT ac.user FROM UserAppCode ac WHERE ac.appCode.name = ?1 and ac.user.email = ?2")
	Users findByAppCodeNameAndUserEmail(String appCode, String emails);
}
