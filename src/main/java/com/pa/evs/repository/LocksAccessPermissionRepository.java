package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.LocksAccessPermisison;

@Repository
public interface LocksAccessPermissionRepository extends JpaRepository<LocksAccessPermisison, Long> {

}
