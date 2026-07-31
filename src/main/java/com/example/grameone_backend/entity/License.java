package com.example.grameone_backend.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "licenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class License {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String activationCode; // e.g. "GRAME-8X92-K4M1"

    private String deviceId; // Registered mobile device ID

    private Integer subscriptionDurationMonths; // 4, 8, 12 months for ORDINARY

    @Column(name = "license_type", columnDefinition = "VARCHAR(255) DEFAULT 'ORDINARY'")
    @Builder.Default
    private String licenseType = "ORDINARY"; // ORDINARY, FREE

    private LocalDateTime validUntil; // Fixed expiration date and time for FREE licenses

    private LocalDateTime activationDate;

    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private String status; // PENDING, ACTIVE, EXPIRED, REVOKED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_plan_id")
    private SubscriptionPlan subscriptionPlan;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = "PENDING";
        }
        if (this.licenseType == null) {
            this.licenseType = "ORDINARY";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
