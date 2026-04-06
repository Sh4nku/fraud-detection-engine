package com.fraud.rule;

import com.fraud.kafka.event.TransactionEvent;
import com.fraud.score.RiskLevel;
import com.fraud.score.RiskScore;
import com.fraud.velocity.VelocityTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FraudRuleEngineTest {

    @Mock
    private VelocityTracker velocityTracker;

    @InjectMocks
    private FraudRuleEngine fraudRuleEngine;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(fraudRuleEngine, "highAmountThreshold", new BigDecimal("50000"));
        ReflectionTestUtils.setField(fraudRuleEngine, "velocityLimit", 5);
        ReflectionTestUtils.setField(fraudRuleEngine, "nightHourStart", 1);
        ReflectionTestUtils.setField(fraudRuleEngine, "nightHourEnd", 4);
        ReflectionTestUtils.setField(fraudRuleEngine, "repeatedDestinationLimit", 3);
    }

    @Test
    void evaluate_HighAmount_ReturnsHighRiskScore() {
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(UUID.randomUUID())
                .sourceAccountNumber("PAY-SOURCE")
                .destinationAccountNumber("PAY-DEST")
                .amount(new BigDecimal("75000"))
                .build();

        when(velocityTracker.incrementAndGetTxnCount(any())).thenReturn(1L);
        when(velocityTracker.incrementAndGetDestCount(any(), any())).thenReturn(1L);

        RiskScore result = fraudRuleEngine.evaluate(event);

        assertThat(result.getScore()).isGreaterThanOrEqualTo(40);
        assertThat(result.getTriggeredRules()).contains("HIGH_AMOUNT");
    }

    @Test
    void evaluate_VelocityExceeded_ReturnsSuspiciousScore() {
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(UUID.randomUUID())
                .sourceAccountNumber("PAY-SOURCE")
                .destinationAccountNumber("PAY-DEST")
                .amount(new BigDecimal("1000"))
                .build();

        when(velocityTracker.incrementAndGetTxnCount(any())).thenReturn(10L);
        when(velocityTracker.incrementAndGetDestCount(any(), any())).thenReturn(1L);

        RiskScore result = fraudRuleEngine.evaluate(event);

        assertThat(result.getTriggeredRules()).contains("VELOCITY_EXCEEDED");
        assertThat(result.getScore()).isGreaterThanOrEqualTo(30);
    }

    @Test
    void evaluate_SafeTransaction_ReturnsSafeScore() {
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(UUID.randomUUID())
                .sourceAccountNumber("PAY-SOURCE")
                .destinationAccountNumber("PAY-DEST")
                .amount(new BigDecimal("500"))
                .build();

        when(velocityTracker.incrementAndGetTxnCount(any())).thenReturn(1L);
        when(velocityTracker.incrementAndGetDestCount(any(), any())).thenReturn(1L);

        RiskScore result = fraudRuleEngine.evaluate(event);

        assertThat(result.getRiskLevel()).isEqualTo(RiskLevel.SAFE);
        assertThat(result.getTriggeredRules()).isEmpty();
    }

    @Test
    void evaluate_RoundAmount_AddsToScore() {
        TransactionEvent event = TransactionEvent.builder()
                .transactionId(UUID.randomUUID())
                .sourceAccountNumber("PAY-SOURCE")
                .destinationAccountNumber("PAY-DEST")
                .amount(new BigDecimal("10000"))
                .build();

        when(velocityTracker.incrementAndGetTxnCount(any())).thenReturn(1L);
        when(velocityTracker.incrementAndGetDestCount(any(), any())).thenReturn(1L);

        RiskScore result = fraudRuleEngine.evaluate(event);

        assertThat(result.getTriggeredRules()).contains("ROUND_AMOUNT");
    }
}
