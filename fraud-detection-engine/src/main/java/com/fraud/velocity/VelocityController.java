package com.fraud.velocity;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/fraud/velocity")
@RequiredArgsConstructor
public class VelocityController {

    private final VelocityTracker velocityTracker;

    @GetMapping("/{accountNumber}")
    public ResponseEntity<Map<String, Object>> getVelocityStats(@PathVariable String accountNumber) {
        long count = velocityTracker.getTxnCount(accountNumber);
        return ResponseEntity.ok(Map.of(
                "accountNumber", accountNumber,
                "transactionCount", count,
                "windowSeconds", 60,
                "isHighVelocity", count > 5
        ));
    }

    @DeleteMapping("/{accountNumber}/reset")
    public ResponseEntity<Map<String, String>> resetVelocity(@PathVariable String accountNumber) {
        velocityTracker.resetVelocity(accountNumber);
        return ResponseEntity.ok(Map.of(
                "message", "Velocity reset for account: " + accountNumber
        ));
    }
}
