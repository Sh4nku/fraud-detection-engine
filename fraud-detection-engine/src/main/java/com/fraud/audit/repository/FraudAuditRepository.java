package com.fraud.audit.repository;

import com.fraud.audit.entity.FraudAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FraudAuditRepository extends JpaRepository<FraudAuditLog, UUID> {

    List<FraudAuditLog> findByFraudCaseIdOrderByAuditedAtDesc(UUID fraudCaseId);

    List<FraudAuditLog> findByTransactionIdOrderByAuditedAtDesc(UUID transactionId);
}
