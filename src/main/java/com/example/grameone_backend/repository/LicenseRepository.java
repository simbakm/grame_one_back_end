package com.example.grameone_backend.repository;

import com.example.grameone_backend.entity.License;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface LicenseRepository extends JpaRepository<License, Long> {
    Optional<License> findByActivationCode(String activationCode);
}
