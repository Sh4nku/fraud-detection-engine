package com.fraud.fraudcase.dto;

import com.fraud.fraudcase.entity.CaseStatus;
import com.fraud.fraudcase.entity.RiskLevel;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public class FraudCaseDto {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class Response {
        private UUID id;
        private UUID transactionId;
        private String sourceAccountNumber;
        private String destinationAccountNumber;
        private BigDecimal amount;
        private int riskScore;
        private RiskLevel riskLevel;
        private String triggeredRules;
        private CaseStatus status;
        private String reviewedBy;
        private String reviewNotes;
        private LocalDateTime detectedAt;
        private LocalDateTime updatedAt;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewRequest {
        @NotNull(message = "Status is required")
        private CaseStatus status;

        @NotBlank(message = "Reviewer name is required")
        private String reviewedBy;

        private String reviewNotes;
    }
}
