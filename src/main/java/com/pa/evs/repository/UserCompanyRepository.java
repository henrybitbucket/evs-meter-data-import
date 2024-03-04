package com.pa.evs.repository;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.UserCompany;

@Repository
public interface UserCompanyRepository extends JpaRepository<UserCompany, Long> {

	List<UserCompany> findByCompanyId(Long cpnId);
	
	@Query("SELECT company.name FROM UserCompany WHERE user.userId = ?1")
	List<String> findCompanyNameByUserId(Long userId);
	
	@Query("SELECT company.name FROM UserCompany WHERE user.email = ?1")
	List<String> findCompanyNameByUserEmail(String email);
	
	@Modifying
	@Query("DELETE UserCompany WHERE id in (SELECT id from UserCompany WHERE user.userId = ?1 and company.appCode.name = ?2 and company.name not in (?3))")
	void deleteNotInCompanies(Long userId, String appCode, Set<String> cpnNames);
}
