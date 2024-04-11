package com.pa.evs.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.pa.evs.model.CountryCode;

public interface CountryCodeRepository extends JpaRepository<CountryCode, Long> {
}
