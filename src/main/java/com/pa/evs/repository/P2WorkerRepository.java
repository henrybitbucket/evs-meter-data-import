package com.pa.evs.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import com.pa.evs.model.P2Worker;
import com.pa.evs.model.Users;


@Repository
public interface P2WorkerRepository extends JpaRepository<P2Worker, Long> {
	List<P2Worker> findByManager(String manager);
	
	Optional<P2Worker> findByManagerAndEmail(String manager, String email);

	@Query("SELECT ur.user FROM UserPermission ur where exists (select 1 from Permission r where r.id = ur.permission.id and r.name = 'P_P2_MANAGER') ")
	List<Users> getP2Managers();
}
