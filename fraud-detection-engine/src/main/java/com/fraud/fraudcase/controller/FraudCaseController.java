package com.fraud.fraudcase.controller;

import com.fraud.fraudcase.dto.FraudCaseDto;
import com.fraud.fraudcase.entity.CaseStatus;
import com.fraud.fraudcase.service.FraudCaseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fraud/cases")
@RequiredArgsConstructor
public class FraudCaseController {

    private final FraudCaseService fraudCaseService;

    @GetMapping
    public ResponseEntity<List<FraudCaseDto.Response>> getAllCases() {
        return ResponseEntity.ok(fraudCaseService.getAllCases());
    }

    @GetMapping("/{caseId}")
    public ResponseEntity<FraudCaseDto.Response> getCaseById(@PathVariable UUID caseId) {
        return ResponseEntity.ok(fraudCaseService.getCaseById(caseId));
    }

    @GetMapping("/transaction/{transactionId}")
    public ResponseEntity<FraudCaseDto.Response> getCaseByTransactionId(@PathVariable UUID transactionId) {
        return ResponseEntity.ok(fraudCaseService.getCaseByTransactionId(transactionId));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<FraudCaseDto.Response>> getCasesByStatus(@PathVariable CaseStatus status) {
        return ResponseEntity.ok(fraudCaseService.getCasesByStatus(status));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<FraudCaseDto.Response>> getCasesByAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(fraudCaseService.getCasesByAccount(accountNumber));
    }

    @PatchMapping("/{caseId}/review")
    public ResponseEntity<FraudCaseDto.Response> reviewCase(
            @PathVariable UUID caseId,
            @Valid @RequestBody FraudCaseDto.ReviewRequest request) {
        return ResponseEntity.ok(fraudCaseService.reviewCase(caseId, request));
    }
}
