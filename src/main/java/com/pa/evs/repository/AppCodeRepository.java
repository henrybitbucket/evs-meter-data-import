package com.pa.evs.repository;

import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.AppCode;

@Repository
public interface AppCodeRepository extends JpaRepository<AppCode, Long> {

	AppCode findByName(String name);

	List<AppCode> findByNameIn(Collection<String> names);
}
