package com.fintech.riskops.controller;

import com.fintech.riskops.dto.response.AuditLogResponse;
import com.fintech.riskops.enums.ActionType;
import com.fintech.riskops.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
public class AuditLogController {

    private final AuditLogService auditLogService;

    /**
     * Paginated, multi-filter audit log search.
     * All params optional - omit to get all logs.
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ANALYST', 'RISK_OPS', 'ADMIN')")
    public ResponseEntity<Page<AuditLogResponse>> search(
        @RequestParam(required = false) ActionType actionType,
        @RequestParam(required = false) String entityType,
        @RequestParam(required = false) String performedBy,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
        @PageableDefault(size = 50, sort = "timestamp", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(
            auditLogService.search(actionType, entityType, performedBy, from, to, pageable)
        );
    }

    /**
     * Full audit history for a specific entity (e.g., all changes to merchant X).
     */
    @GetMapping("/{entityType}/{entityId}")
    @PreAuthorize("hasAnyRole('ANALYST', 'RISK_OPS', 'ADMIN')")
    public ResponseEntity<List<AuditLogResponse>> getEntityHistory(
        @PathVariable String entityType,
        @PathVariable String entityId
    ) {
        return ResponseEntity.ok(auditLogService.getEntityHistory(entityType, entityId));
    }

    /**
     * CSV export for compliance team.
     * Returns a downloadable CSV file.
     */
    @GetMapping("/export/csv")
    @PreAuthorize("hasRole('ADMIN') or hasRole('COMPLIANCE')")
    public ResponseEntity<byte[]> exportCsv(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to
    ) {
        String csv = auditLogService.exportToCsv(from, to);

        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"audit-log-export.csv\"")
            .contentType(MediaType.parseMediaType("text/csv"))
            .body(csv.getBytes());
    }
}
