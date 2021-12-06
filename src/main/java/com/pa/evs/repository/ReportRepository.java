package com.pa.evs.repository;

import com.pa.evs.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    void deleteById(Long id);
    Optional<Report> findById(Long id);
}
