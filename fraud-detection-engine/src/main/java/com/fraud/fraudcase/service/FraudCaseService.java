package com.fraud.fraudcase.service;

import com.fraud.exception.ErrorCode;
import com.fraud.exception.FraudException;
import com.fraud.fraudcase.dto.FraudCaseDto;
import com.fraud.fraudcase.entity.CaseStatus;
import com.fraud.fraudcase.entity.FraudCase;
import com.fraud.fraudcase.entity.RiskLevel;
import com.fraud.fraudcase.repository.FraudCaseRepository;
import com.fraud.kafka.event.FraudAlertEvent;
import com.fraud.kafka.event.TransactionEvent;
import com.fraud.kafka.producer.FraudAlertProducer;
import com.fraud.rule.FraudRuleEngine;
import com.fraud.score.RiskScore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudCaseService {

    private final FraudCaseRepository fraudCaseRepository;
    private final FraudRuleEngine fraudRuleEngine;
    private final FraudAlertProducer fraudAlertProducer;

    @Transactional
    public void processTransaction(TransactionEvent event) {

        // Skip if already processed
        if (fraudCaseRepository.existsByTransactionId(event.getTransactionId())) {
            log.warn("Transaction already processed: {}", event.getTransactionId());
            return;
        }

        // Evaluate fraud rules
        RiskScore riskScore = fraudRuleEngine.evaluate(event);

        // Only create case if not SAFE
        if (riskScore.getRiskLevel() == com.fraud.score.RiskLevel.SAFE) {
            log.info("Transaction {} is SAFE with score {}", event.getTransactionId(), riskScore.getScore());
            return;
        }

        // Create fraud case
        FraudCase fraudCase = FraudCase.builder()
                .transactionId(event.getTransactionId())
                .sourceAccountNumber(event.getSourceAccountNumber())
                .destinationAccountNumber(event.getDestinationAccountNumber())
                .amount(event.getAmount())
                .riskScore(riskScore.getScore())
                .riskLevel(RiskLevel.valueOf(riskScore.getRiskLevel().name()))
                .triggeredRules(String.join(",", riskScore.getTriggeredRules()))
                .status(CaseStatus.OPEN)
                .build();

        FraudCase saved = fraudCaseRepository.save(fraudCase);
        log.warn("Fraud case created: {} for transaction: {} risk: {}",
                saved.getId(), event.getTransactionId(), riskScore.getRiskLevel());

        // Publish fraud alert back to Kafka
        FraudAlertEvent alertEvent = FraudAlertEvent.builder()
                .fraudCaseId(saved.getId())
                .transactionId(event.getTransactionId())
                .sourceAccountNumber(event.getSourceAccountNumber())
                .destinationAccountNumber(event.getDestinationAccountNumber())
                .amount(event.getAmount())
                .riskScore(riskScore.getScore())
                .riskLevel(riskScore.getRiskLevel().name())
                .triggeredRules(String.join(",", riskScore.getTriggeredRules()))
                .detectedAt(LocalDateTime.now())
                .build();

        fraudAlertProducer.publishFraudAlert(alertEvent);
    }

    @Transactional(readOnly = true)
    public List<FraudCaseDto.Response> getAllCases() {
        return fraudCaseRepository.findAll()
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public FraudCaseDto.Response getCaseById(UUID caseId) {
        FraudCase fraudCase = fraudCaseRepository.findById(caseId)
                .orElseThrow(() -> new FraudException(ErrorCode.FRAUD_CASE_NOT_FOUND,
                        "Fraud case not found: " + caseId,
                        HttpStatus.NOT_FOUND));
        return mapToResponse(fraudCase);
    }

    @Transactional(readOnly = true)
    public FraudCaseDto.Response getCaseByTransactionId(UUID transactionId) {
        FraudCase fraudCase = fraudCaseRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new FraudException(ErrorCode.FRAUD_CASE_NOT_FOUND,
                        "Fraud case not found for transaction: " + transactionId,
                        HttpStatus.NOT_FOUND));
        return mapToResponse(fraudCase);
    }

    @Transactional(readOnly = true)
    public List<FraudCaseDto.Response> getCasesByStatus(CaseStatus status) {
        return fraudCaseRepository.findByStatus(status)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public FraudCaseDto.Response reviewCase(UUID caseId, FraudCaseDto.ReviewRequest request) {
        FraudCase fraudCase = fraudCaseRepository.findById(caseId)
                .orElseThrow(() -> new FraudException(ErrorCode.FRAUD_CASE_NOT_FOUND,
                        "Fraud case not found: " + caseId,
                        HttpStatus.NOT_FOUND));

        fraudCase.setStatus(request.getStatus());
        fraudCase.setReviewedBy(request.getReviewedBy());
        fraudCase.setReviewNotes(request.getReviewNotes());

        FraudCase updated = fraudCaseRepository.save(fraudCase);
        log.info("Fraud case {} reviewed by {} status: {}",
                caseId, request.getReviewedBy(), request.getStatus());

        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<FraudCaseDto.Response> getCasesByAccount(String accountNumber) {
        return fraudCaseRepository.findBySourceAccountNumberOrderByDetectedAtDesc(accountNumber)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private FraudCaseDto.Response mapToResponse(FraudCase fraudCase) {
        return FraudCaseDto.Response.builder()
                .id(fraudCase.getId())
                .transactionId(fraudCase.getTransactionId())
                .sourceAccountNumber(fraudCase.getSourceAccountNumber())
                .destinationAccountNumber(fraudCase.getDestinationAccountNumber())
                .amount(fraudCase.getAmount())
                .riskScore(fraudCase.getRiskScore())
                .riskLevel(fraudCase.getRiskLevel())
                .triggeredRules(fraudCase.getTriggeredRules())
                .status(fraudCase.getStatus())
                .reviewedBy(fraudCase.getReviewedBy())
                .reviewNotes(fraudCase.getReviewNotes())
                .detectedAt(fraudCase.getDetectedAt())
                .updatedAt(fraudCase.getUpdatedAt())
                .build();
    }
}
