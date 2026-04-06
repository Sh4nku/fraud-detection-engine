package com.fraud.rule;

import com.fraud.kafka.event.TransactionEvent;
import com.fraud.score.RiskScore;
import com.fraud.velocity.VelocityTracker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudRuleEngine {

    private final VelocityTracker velocityTracker;

    @Value("${fraud.rules.high-amount-threshold}")
    private BigDecimal highAmountThreshold;

    @Value("${fraud.rules.velocity-limit}")
    private int velocityLimit;

    @Value("${fraud.rules.night-hour-start}")
    private int nightHourStart;

    @Value("${fraud.rules.night-hour-end}")
    private int nightHourEnd;

    @Value("${fraud.rules.repeated-destination-limit}")
    private int repeatedDestinationLimit;

    public RiskScore evaluate(TransactionEvent event) {
        List<String> triggeredRules = new ArrayList<>();
        int totalScore = 0;

        // Rule 1: High amount check
        if (event.getAmount().compareTo(highAmountThreshold) > 0) {
            triggeredRules.add("HIGH_AMOUNT");
            totalScore += 40;
            log.info("Rule triggered: HIGH_AMOUNT for txn: {}", event.getTransactionId());
        }

        // Rule 2: Velocity check
        long txnCount = velocityTracker.incrementAndGetTxnCount(event.getSourceAccountNumber());
        if (txnCount > velocityLimit) {
            triggeredRules.add("VELOCITY_EXCEEDED");
            totalScore += 30;
            log.info("Rule triggered: VELOCITY_EXCEEDED count:{} for account: {}", txnCount, event.getSourceAccountNumber());
        }

        // Rule 3: Night time transaction
        int currentHour = LocalTime.now().getHour();
        if (currentHour >= nightHourStart && currentHour <= nightHourEnd) {
            triggeredRules.add("NIGHT_TIME_TRANSACTION");
            totalScore += 20;
            log.info("Rule triggered: NIGHT_TIME_TRANSACTION for txn: {}", event.getTransactionId());
        }

        // Rule 4: Round amount check
        if (isRoundAmount(event.getAmount())) {
            triggeredRules.add("ROUND_AMOUNT");
            totalScore += 10;
            log.info("Rule triggered: ROUND_AMOUNT for txn: {}", event.getTransactionId());
        }

        // Rule 5: Repeated destination check
        long destCount = velocityTracker.incrementAndGetDestCount(
                event.getSourceAccountNumber(),
                event.getDestinationAccountNumber());
        if (destCount > repeatedDestinationLimit) {
            triggeredRules.add("REPEATED_DESTINATION");
            totalScore += 25;
            log.info("Rule triggered: REPEATED_DESTINATION count:{} for txn: {}", destCount, event.getTransactionId());
        }

        // Cap score at 100
        totalScore = Math.min(totalScore, 100);

        RiskScore riskScore = RiskScore.builder()
                .score(totalScore)
                .riskLevel(RiskScore.fromScore(totalScore))
                .triggeredRules(triggeredRules)
                .build();

        log.info("Risk evaluation complete for txn: {} score: {} level: {}",
                event.getTransactionId(), totalScore, riskScore.getRiskLevel());

        return riskScore;
    }

    private boolean isRoundAmount(BigDecimal amount) {
        return amount.remainder(new BigDecimal("1000")).compareTo(BigDecimal.ZERO) == 0;
    }
}
