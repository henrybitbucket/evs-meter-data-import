package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.Company;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {

	List<Company> findByNameIn(Collection<String> names);
	
	Optional<Company> findByNameAndAppCodeName(String name, String appCodeName);

	@Query(value = "From Company where name = ?1 and appCode.name = ?2 and id <> ?3")
	List<Company> findByNameAndAppCodeAndDifferenceId(String name, String appCpde, Long id);

	List<Company> findByIdIn(List<Long> cpnIds);

	List<Company> findByNameIn(List<String> cpnNames);
}
