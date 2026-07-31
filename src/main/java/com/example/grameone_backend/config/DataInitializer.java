package com.example.grameone_backend.config;

import com.example.grameone_backend.entity.Grade;
import com.example.grameone_backend.entity.SubscriptionPlan;
import com.example.grameone_backend.entity.User;
import com.example.grameone_backend.repository.GradeRepository;
import com.example.grameone_backend.repository.SubscriptionPlanRepository;
import com.example.grameone_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final GradeRepository gradeRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        // 1. Seed Admin User
        if (userRepository.count() == 0) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@grameone.com")
                    .password(passwordEncoder.encode("admin123"))
                    .role("ROLE_ADMIN")
                    .build();
            userRepository.save(admin);
            System.out.println("✅ Seeded default admin user: admin / admin123");
        }

        // 2. Seed Default Grades (Grade 1 to Grade 7)
        if (gradeRepository.count() == 0) {
            List<Grade> grades = List.of(
                    Grade.builder().name("Grade 1").code("GRADE_1").sortOrder(1).description("Grade 1 Curriculum").build(),
                    Grade.builder().name("Grade 2").code("GRADE_2").sortOrder(2).description("Grade 2 Curriculum").build(),
                    Grade.builder().name("Grade 3").code("GRADE_3").sortOrder(3).description("Grade 3 Curriculum").build(),
                    Grade.builder().name("Grade 4").code("GRADE_4").sortOrder(4).description("Grade 4 Curriculum").build(),
                    Grade.builder().name("Grade 5").code("GRADE_5").sortOrder(5).description("Grade 5 Curriculum").build(),
                    Grade.builder().name("Grade 6").code("GRADE_6").sortOrder(6).description("Grade 6 Curriculum").build(),
                    Grade.builder().name("Grade 7").code("GRADE_7").sortOrder(7).description("Grade 7 Curriculum").build()
            );
            gradeRepository.saveAll(grades);
            System.out.println("✅ Seeded Grades 1 through 7");
        }

        // 3. Seed Subscription Plans (4, 8, 12 months)
        if (subscriptionPlanRepository.count() == 0) {
            List<SubscriptionPlan> plans = List.of(
                    SubscriptionPlan.builder()
                            .name("4 Months Plan")
                            .code("PLAN_4M")
                            .durationMonths(4)
                            .price(new BigDecimal("15.00"))
                            .entitlementRules("Access to 1 grade package for 4 months")
                            .isActive(true)
                            .build(),
                    SubscriptionPlan.builder()
                            .name("8 Months Plan")
                            .code("PLAN_8M")
                            .durationMonths(8)
                            .price(new BigDecimal("28.00"))
                            .entitlementRules("Access to 1 grade package for 8 months")
                            .isActive(true)
                            .build(),
                    SubscriptionPlan.builder()
                            .name("12 Months Plan")
                            .code("PLAN_12M")
                            .durationMonths(12)
                            .price(new BigDecimal("40.00"))
                            .entitlementRules("Full access to 1 grade package for 1 year")
                            .isActive(true)
                            .build()
            );
            subscriptionPlanRepository.saveAll(plans);
            System.out.println("✅ Seeded Subscription Plans (4, 8, 12 months)");
        }
    }
}
