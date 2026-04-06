package com.fraud.score;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RiskScore {
    private int score;
    private RiskLevel riskLevel;
    private List<String> triggeredRules;

    public static RiskLevel fromScore(int score) {
        if (score <= 30) return RiskLevel.SAFE;
        if (score <= 60) return RiskLevel.SUSPICIOUS;
        if (score <= 80) return RiskLevel.HIGH_RISK;
        return RiskLevel.FRAUD;
    }
}
