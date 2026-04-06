package com.fraud.fraudcase.repository;

import com.fraud.fraudcase.entity.CaseStatus;
import com.fraud.fraudcase.entity.FraudCase;
import com.fraud.fraudcase.entity.RiskLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FraudCaseRepository extends JpaRepository<FraudCase, UUID> {

    Optional<FraudCase> findByTransactionId(UUID transactionId);

    List<FraudCase> findByStatus(CaseStatus status);

    List<FraudCase> findByRiskLevel(RiskLevel riskLevel);

    List<FraudCase> findBySourceAccountNumberOrderByDetectedAtDesc(String accountNumber);

    boolean existsByTransactionId(UUID transactionId);
}
