package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.Address;

public interface AddressRepository extends JpaRepository<Address, Long> {

}
