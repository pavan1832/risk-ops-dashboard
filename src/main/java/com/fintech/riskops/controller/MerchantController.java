package com.fintech.riskops.controller;

import com.fintech.riskops.dto.request.BulkActionRequest;
import com.fintech.riskops.dto.request.CreateMerchantRequest;
import com.fintech.riskops.dto.response.BulkActionResponse;
import com.fintech.riskops.dto.response.DashboardMetricsResponse;
import com.fintech.riskops.dto.response.MerchantResponse;
import com.fintech.riskops.enums.RiskStatus;
import com.fintech.riskops.service.MerchantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API for merchant management and bulk operations.
 *
 * Role-based access:
 * - ANALYST: read access + risk evaluation
 * - RISK_OPS: all of above + bulk actions
 * - ADMIN: full access including delete
 */
@RestController
@RequestMapping("/api/v1/merchants")
@RequiredArgsConstructor
public class MerchantController {

    private final MerchantService merchantService;

    // ---- CRUD ----

    @PostMapping
    @PreAuthorize("hasRole('RISK_OPS') or hasRole('ADMIN')")
    public ResponseEntity<MerchantResponse> create(@Valid @RequestBody CreateMerchantRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(merchantService.createMerchant(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ANALYST', 'RISK_OPS', 'ADMIN')")
    public ResponseEntity<MerchantResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(merchantService.getMerchant(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'RISK_OPS', 'ADMIN')")
    public ResponseEntity<Page<MerchantResponse>> list(
        @RequestParam(required = false) RiskStatus status,
        @RequestParam(required = false) String name,
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(merchantService.getMerchants(status, name, pageable));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('RISK_OPS') or hasRole('ADMIN')")
    public ResponseEntity<MerchantResponse> update(
        @PathVariable UUID id,
        @Valid @RequestBody CreateMerchantRequest req
    ) {
        return ResponseEntity.ok(merchantService.updateMerchant(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        merchantService.deleteMerchant(id);
        return ResponseEntity.noContent().build();
    }

    // ---- BULK OPERATIONS ----

    @PostMapping("/bulk/freeze")
    @PreAuthorize("hasRole('RISK_OPS') or hasRole('ADMIN')")
    public ResponseEntity<BulkActionResponse> bulkFreeze(@Valid @RequestBody BulkActionRequest req) {
        return ResponseEntity.ok(merchantService.bulkFreeze(req));
    }

    @PostMapping("/bulk/unfreeze")
    @PreAuthorize("hasRole('RISK_OPS') or hasRole('ADMIN')")
    public ResponseEntity<BulkActionResponse> bulkUnfreeze(@Valid @RequestBody BulkActionRequest req) {
        return ResponseEntity.ok(merchantService.bulkUnfreeze(req));
    }

    @PostMapping("/bulk/feature/enable")
    @PreAuthorize("hasRole('RISK_OPS') or hasRole('ADMIN')")
    public ResponseEntity<BulkActionResponse> bulkEnableFeature(@Valid @RequestBody BulkActionRequest req) {
        return ResponseEntity.ok(merchantService.bulkEnableFeature(req));
    }

    @PostMapping("/bulk/feature/disable")
    @PreAuthorize("hasRole('RISK_OPS') or hasRole('ADMIN')")
    public ResponseEntity<BulkActionResponse> bulkDisableFeature(@Valid @RequestBody BulkActionRequest req) {
        return ResponseEntity.ok(merchantService.bulkDisableFeature(req));
    }

    // ---- DASHBOARD ----

    @GetMapping("/dashboard/metrics")
    @PreAuthorize("hasAnyRole('ANALYST', 'RISK_OPS', 'ADMIN')")
    public ResponseEntity<DashboardMetricsResponse> dashboardMetrics() {
        return ResponseEntity.ok(merchantService.getDashboardMetrics());
    }
}
