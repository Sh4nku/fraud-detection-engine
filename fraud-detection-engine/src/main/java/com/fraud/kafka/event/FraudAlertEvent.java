package com.fraud.kafka.event;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlertEvent {
    private UUID fraudCaseId;
    private UUID transactionId;
    private String sourceAccountNumber;
    private String destinationAccountNumber;
    private BigDecimal amount;
    private int riskScore;
    private String riskLevel;
    private String triggeredRules;
    private LocalDateTime detectedAt;
}
