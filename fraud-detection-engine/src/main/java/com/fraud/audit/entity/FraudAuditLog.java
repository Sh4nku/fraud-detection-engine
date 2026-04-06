package com.fraud.audit.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "fraud_audit_logs")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID fraudCaseId;

    @Column(nullable = false)
    private UUID transactionId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false)
    private String performedBy;

    private String details;

    @CreationTimestamp
    private LocalDateTime auditedAt;
}
