package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.AddressLog;

@Repository
public interface AddressLogRepository extends JpaRepository<AddressLog, Long> {

}
