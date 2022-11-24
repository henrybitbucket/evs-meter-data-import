package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.Setting;

@Repository
public interface SettingRepository extends JpaRepository<Setting, Long>{
	
	List<Setting> findAll();
	
	Optional<Setting> findByKey(String key);
}
