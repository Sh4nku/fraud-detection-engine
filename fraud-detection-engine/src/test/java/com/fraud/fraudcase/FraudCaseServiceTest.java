package com.fraud.fraudcase;

import com.fraud.fraudcase.dto.FraudCaseDto;
import com.fraud.fraudcase.entity.CaseStatus;
import com.fraud.fraudcase.entity.FraudCase;
import com.fraud.fraudcase.entity.RiskLevel;
import com.fraud.fraudcase.repository.FraudCaseRepository;
import com.fraud.fraudcase.service.FraudCaseService;
import com.fraud.kafka.event.TransactionEvent;
import com.fraud.kafka.producer.FraudAlertProducer;
import com.fraud.rule.FraudRuleEngine;
import com.fraud.score.RiskScore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FraudCaseServiceTest {

    @Mock
    private FraudCaseRepository fraudCaseRepository;

    @Mock
    private FraudRuleEngine fraudRuleEngine;

    @Mock
    private FraudAlertProducer fraudAlertProducer;

    @InjectMocks
    private FraudCaseService fraudCaseService;

    private TransactionEvent mockEvent;
    private FraudCase mockFraudCase;

    @BeforeEach
    void setUp() {
        mockEvent = TransactionEvent.builder()
                .transactionId(UUID.randomUUID())
                .sourceAccountNumber("PAY-SOURCE")
                .destinationAccountNumber("PAY-DEST")
                .amount(new BigDecimal("75000"))
                .build();

        mockFraudCase = FraudCase.builder()
                .id(UUID.randomUUID())
                .transactionId(mockEvent.getTransactionId())
                .sourceAccountNumber("PAY-SOURCE")
                .destinationAccountNumber("PAY-DEST")
                .amount(new BigDecimal("75000"))
                .riskScore(70)
                .riskLevel(RiskLevel.HIGH_RISK)
                .triggeredRules("HIGH_AMOUNT")
                .status(CaseStatus.OPEN)
                .build();
    }

    @Test
    void processTransaction_HighRisk_CreatesFraudCase() {
        RiskScore riskScore = RiskScore.builder()
                .score(70)
                .riskLevel(com.fraud.score.RiskLevel.HIGH_RISK)
                .triggeredRules(List.of("HIGH_AMOUNT"))
                .build();

        when(fraudCaseRepository.existsByTransactionId(any())).thenReturn(false);
        when(fraudRuleEngine.evaluate(any())).thenReturn(riskScore);
        when(fraudCaseRepository.save(any())).thenReturn(mockFraudCase);

        fraudCaseService.processTransaction(mockEvent);

        verify(fraudCaseRepository, times(1)).save(any());
        verify(fraudAlertProducer, times(1)).publishFraudAlert(any());
    }

    @Test
    void processTransaction_Safe_DoesNotCreateCase() {
        RiskScore safeScore = RiskScore.builder()
                .score(10)
                .riskLevel(com.fraud.score.RiskLevel.SAFE)
                .triggeredRules(List.of())
                .build();

        when(fraudCaseRepository.existsByTransactionId(any())).thenReturn(false);
        when(fraudRuleEngine.evaluate(any())).thenReturn(safeScore);

        fraudCaseService.processTransaction(mockEvent);

        verify(fraudCaseRepository, never()).save(any());
        verify(fraudAlertProducer, never()).publishFraudAlert(any());
    }

    @Test
    void processTransaction_Duplicate_Skipped() {
        when(fraudCaseRepository.existsByTransactionId(any())).thenReturn(true);

        fraudCaseService.processTransaction(mockEvent);

        verify(fraudRuleEngine, never()).evaluate(any());
        verify(fraudCaseRepository, never()).save(any());
    }

    @Test
    void reviewCase_Success() {
        FraudCaseDto.ReviewRequest request = FraudCaseDto.ReviewRequest.builder()
                .status(CaseStatus.CONFIRMED_FRAUD)
                .reviewedBy("analyst@hsbc.com")
                .reviewNotes("Confirmed fraudulent transaction")
                .build();

        when(fraudCaseRepository.findById(any())).thenReturn(Optional.of(mockFraudCase));
        when(fraudCaseRepository.save(any())).thenReturn(mockFraudCase);

        FraudCaseDto.Response response = fraudCaseService.reviewCase(mockFraudCase.getId(), request);

        assertThat(response).isNotNull();
        verify(fraudCaseRepository, times(1)).save(any());
    }
}
