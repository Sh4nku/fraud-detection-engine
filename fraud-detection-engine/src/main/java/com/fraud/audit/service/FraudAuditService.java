package com.fraud.audit.service;

import com.fraud.audit.entity.FraudAuditLog;
import com.fraud.audit.repository.FraudAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAuditService {

    private final FraudAuditRepository fraudAuditRepository;

    @Transactional
    public void log(UUID fraudCaseId, UUID transactionId,
                    String action, String performedBy, String details) {
        FraudAuditLog auditLog = FraudAuditLog.builder()
                .fraudCaseId(fraudCaseId)
                .transactionId(transactionId)
                .action(action)
                .performedBy(performedBy)
                .details(details)
                .build();
        fraudAuditRepository.save(auditLog);
        log.info("Fraud audit log saved: {} for case: {}", action, fraudCaseId);
    }

    @Transactional(readOnly = true)
    public List<FraudAuditLog> getAuditsByCase(UUID fraudCaseId) {
        return fraudAuditRepository.findByFraudCaseIdOrderByAuditedAtDesc(fraudCaseId);
    }

    @Transactional(readOnly = true)
    public List<FraudAuditLog> getAuditsByTransaction(UUID transactionId) {
        return fraudAuditRepository.findByTransactionIdOrderByAuditedAtDesc(transactionId);
    }
}
