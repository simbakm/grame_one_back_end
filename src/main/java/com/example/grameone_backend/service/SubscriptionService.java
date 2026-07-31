package com.example.grameone_backend.service;

import com.example.grameone_backend.entity.SubscriptionPlan;
import com.example.grameone_backend.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final SubscriptionPlanRepository planRepository;

    public List<SubscriptionPlan> getAllPlans() {
        return planRepository.findAll();
    }

    public SubscriptionPlan createPlan(SubscriptionPlan plan) {
        return planRepository.save(plan);
    }

    public SubscriptionPlan updatePlan(Long id, SubscriptionPlan details) {
        SubscriptionPlan plan = planRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan not found"));
        plan.setName(details.getName());
        plan.setCode(details.getCode());
        plan.setDurationMonths(details.getDurationMonths());
        plan.setPrice(details.getPrice());
        plan.setEntitlementRules(details.getEntitlementRules());
        plan.setIsActive(details.getIsActive());
        return planRepository.save(plan);
    }

    public void deletePlan(Long id) {
        planRepository.deleteById(id);
    }
}
