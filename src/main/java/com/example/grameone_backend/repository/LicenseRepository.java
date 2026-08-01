package com.example.grameone_backend.repository;

import com.example.grameone_backend.entity.License;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Long> {
    Optional<License> findByActivationCode(String activationCode);

    long countByStatus(String status);

    List<License> findTop5ByStatusOrderByActivationDateDesc(String status);

    /** Returns [year, month, count] rows for licenses activated after the given date */
    @Query("SELECT YEAR(l.activationDate), MONTH(l.activationDate), COUNT(l) " +
           "FROM License l " +
           "WHERE l.activationDate >= :since AND l.activationDate IS NOT NULL " +
           "GROUP BY YEAR(l.activationDate), MONTH(l.activationDate) " +
           "ORDER BY YEAR(l.activationDate), MONTH(l.activationDate)")
    List<Object[]> countActivationsByMonth(@Param("since") LocalDateTime since);
}
