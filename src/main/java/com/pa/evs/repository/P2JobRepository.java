package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.P2Job;

@Repository
public interface P2JobRepository extends JpaRepository<P2Job, Long> {


}
