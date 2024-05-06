package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.DMSMcAcc;

@Repository
public interface DMSAccRepository extends JpaRepository<DMSMcAcc, Long> {

	Object findByEmail(String email);

	DMSMcAcc findByPhoneNumber(String phone);

}
