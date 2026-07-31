package com.example.grameone_backend.repository;

import com.example.grameone_backend.entity.SubscriptionPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SubscriptionPlanRepository extends JpaRepository<SubscriptionPlan, Long> {
    Optional<SubscriptionPlan> findByCodeIgnoreCase(String code);
}
