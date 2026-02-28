package com.fintech.riskops.controller;

import com.fintech.riskops.dto.response.RiskEvaluationResponse;
import com.fintech.riskops.service.RiskEvaluationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
public class RiskController {

    private final RiskEvaluationService riskEvaluationService;

    /**
     * Trigger on-demand risk evaluation for a merchant.
     * In production, this also runs automatically after each transaction.
     */
    @PostMapping("/evaluate/{merchantId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'RISK_OPS', 'ADMIN')")
    public ResponseEntity<RiskEvaluationResponse> evaluate(@PathVariable UUID merchantId) {
        return ResponseEntity.ok(riskEvaluationService.evaluate(merchantId));
    }
}
