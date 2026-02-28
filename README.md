# Risk Operations Automation Dashboard

> **Fintech-grade internal risk operations tool built with Java 17 + Spring Boot**  
> Replaces Excel-based risk trackers with a system-driven dashboard featuring bulk controls, immutable audit trails, and a configurable rule-based risk engine.

---

## System Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                    RISK OPS DASHBOARD                               │
│                                                                     │
│  ┌──────────────┐    REST/JSON    ┌──────────────────────────────┐  │
│  │  HTML/JS     │◄──────────────►│  Spring Boot Application     │  │
│  │  Dashboard   │                │                              │  │
│  │  (index.html)│                │  ┌─────────────────────────┐ │  │
│  └──────────────┘                │  │   Controllers (Layer 1) │ │  │
│                                  │  │  MerchantController     │ │  │
│  ┌──────────────┐                │  │  RiskController         │ │  │
│  │  API Clients │                │  │  AuditLogController     │ │  │
│  │  (cURL/      │                │  └──────────┬──────────────┘ │  │
│  │  Postman)    │                │             │                │  │
│  └──────────────┘                │  ┌──────────▼──────────────┐ │  │
│                                  │  │   Services (Layer 2)    │ │  │
│                                  │  │  MerchantService        │ │  │
│                                  │  │  RiskEvaluationService  │ │  │
│                                  │  │  AuditLogService        │ │  │
│                                  │  └──────────┬──────────────┘ │  │
│                                  │             │                │  │
│                                  │  ┌──────────▼──────────────┐ │  │
│                                  │  │  Risk Engine (Layer 2b) │ │  │
│                                  │  │  RiskEngine (orchestrat)│ │  │
│                                  │  │  VelocityRule           │ │  │
│                                  │  │  AmountThresholdRule    │ │  │
│                                  │  │  FrequencyRule          │ │  │
│                                  │  │  [+ new rules easily]   │ │  │
│                                  │  └──────────┬──────────────┘ │  │
│                                  │             │                │  │
│                                  │  ┌──────────▼──────────────┐ │  │
│                                  │  │  Repositories (Layer 3) │ │  │
│                                  │  │  Spring Data JPA        │ │  │
│                                  │  └──────────┬──────────────┘ │  │
│                                  └─────────────┼────────────────┘  │
│                                                │                    │
│                                  ┌─────────────▼────────────────┐  │
│                                  │   Database                   │  │
│                                  │   PostgreSQL / H2 (dev)      │  │
│                                  │                              │  │
│                                  │  merchants                   │  │
│                                  │  merchant_features           │  │
│                                  │  transactions                │  │
│                                  │  audit_logs                  │  │
│                                  └──────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────┘
```

---

## Database Schema

```sql
-- Core merchant table
CREATE TABLE merchants (
    id            UUID PRIMARY KEY,
    name          VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL UNIQUE,
    business_type VARCHAR(100),
    risk_status   VARCHAR(20)  NOT NULL,  -- LOW/MEDIUM/HIGH/FROZEN
    risk_score    INT          NOT NULL DEFAULT 0,
    review_pending BOOLEAN     NOT NULL DEFAULT false,
    flagged_at    TIMESTAMP,
    resolved_at   TIMESTAMP,
    created_at    TIMESTAMP,
    updated_at    TIMESTAMP,
    INDEX idx_merchant_risk_status (risk_status),
    INDEX idx_merchant_created_at (created_at)
);

-- Feature toggles: separate table for queryability
CREATE TABLE merchant_features (
    merchant_id UUID        NOT NULL REFERENCES merchants(id),
    feature     VARCHAR(50) NOT NULL,  -- PAYMENTS/WITHDRAWALS/API_ACCESS
    PRIMARY KEY (merchant_id, feature),
    INDEX idx_merchant_feature (merchant_id, feature)
);

-- Transaction history (drives risk engine evaluation)
CREATE TABLE transactions (
    id               UUID PRIMARY KEY,
    merchant_id      UUID           NOT NULL REFERENCES merchants(id),
    amount           DECIMAL(19, 4) NOT NULL,
    currency         CHAR(3)        NOT NULL DEFAULT 'USD',
    transaction_type VARCHAR(50),
    flagged          BOOLEAN        NOT NULL DEFAULT false,
    flag_reason      VARCHAR(500),
    created_at       TIMESTAMP,
    -- Critical composite index for risk queries
    INDEX idx_txn_merchant_created (merchant_id, created_at),
    INDEX idx_txn_amount (amount)
);

-- Immutable audit trail
CREATE TABLE audit_logs (
    id             UUID PRIMARY KEY,
    action_type    VARCHAR(50)  NOT NULL,
    entity_type    VARCHAR(100) NOT NULL,
    entity_id      VARCHAR(36)  NOT NULL,
    old_value      TEXT,                    -- JSON snapshot before change
    new_value      TEXT,                    -- JSON snapshot after change
    performed_by   VARCHAR(255) NOT NULL,   -- username or SYSTEM
    correlation_id VARCHAR(36),             -- links bulk operation records
    description    VARCHAR(1000),
    timestamp      TIMESTAMP    NOT NULL,
    -- NO update/delete ever touches this table
    INDEX idx_audit_entity      (entity_type, entity_id),
    INDEX idx_audit_action_type (action_type),
    INDEX idx_audit_performed_by (performed_by),
    INDEX idx_audit_timestamp   (timestamp),
    INDEX idx_audit_correlation (correlation_id)
);
```

**Design decisions:**
- UUID primary keys: prevent sequential ID enumeration in APIs
- `audit_logs` has no foreign key to `merchants`: records survive merchant deletion
- `correlation_id` in audit_logs groups all records from one bulk operation
- `DECIMAL(19,4)` for amounts: never use float/double for money
- Composite index `(merchant_id, created_at)` critical for sub-millisecond velocity queries

---

## Risk Engine Design

The engine uses the **Strategy Pattern** — each risk dimension is an independent `RiskRule` implementation:

```
RiskEngine (orchestrator)
  ├── VelocityRule       (weight: 40) — txns per hour
  ├── AmountThresholdRule (weight: 35) — single txn + daily volume
  └── FrequencyRule      (weight: 25) — total daily txn count
       Total max score = 100
```

**Thresholds (configurable in application.yml):**
```
Score 0–39  → LOW risk
Score 40–69 → MEDIUM risk
Score 70–89 → HIGH risk
Score 90+   → AUTO-FREEZE
```

**Adding a new rule:** Create a `@Component` implementing `RiskRule`. Spring auto-registers it. Zero changes to the engine.

---

## Quick Start

### Prerequisites
- Java 17+
- Maven 3.8+

### Run locally (H2 in-memory DB — no Postgres needed)
```bash
cd risk-ops-dashboard
mvn spring-boot:run
```

- API: http://localhost:8080
- Dashboard: http://localhost:8080/index.html
- H2 Console: http://localhost:8080/h2-console (JDBC URL: `jdbc:h2:mem:riskopsdb`)

### Run with PostgreSQL
```bash
# Create database
psql -U postgres -c "CREATE DATABASE riskopsdb;"

# Run with postgres profile
mvn spring-boot:run -Dspring-boot.run.profiles=postgres \
  -Dspring-boot.run.arguments="--DB_USERNAME=postgres --DB_PASSWORD=yourpassword"
```

### Run tests
```bash
mvn test
```

---

## API Reference

### Authentication
All endpoints use HTTP Basic Auth.

| User      | Password     | Role       |
|-----------|-------------|------------|
| analyst   | analyst123  | ANALYST    |
| riskops   | riskops123  | RISK_OPS   |
| admin     | admin123    | ADMIN      |

---

### Merchant Endpoints

#### List merchants (with filtering + pagination)
```bash
GET /api/v1/merchants?status=HIGH&name=velocity&page=0&size=20
Authorization: Basic cmhpc29wczpyaXNrb3BzMTIz

Response: {
  "content": [ { "id": "...", "name": "VelocityPay Corp", "riskStatus": "HIGH", ... } ],
  "totalElements": 2, "totalPages": 1
}
```

#### Create merchant
```bash
POST /api/v1/merchants
Authorization: Basic cmhpc29wczpyaXNrb3BzMTIz
Content-Type: application/json

{
  "name": "Acme Payments Ltd",
  "email": "ops@acme.io",
  "businessType": "E_COMMERCE",
  "riskStatus": "LOW",
  "enabledFeatures": ["PAYMENTS", "WITHDRAWALS", "API_ACCESS"]
}
```

#### Bulk freeze
```bash
POST /api/v1/merchants/bulk/freeze
{
  "merchantIds": ["uuid-1", "uuid-2", "uuid-3"],
  "reason": "Suspected coordinated fraud - case #2024-XYZ"
}

Response: {
  "requestedCount": 3,
  "successCount": 3,
  "failedCount": 0,
  "correlationId": "a1b2c3d4-...",   ← links audit log entries
  "message": "Bulk freeze completed: 3/3 merchants frozen"
}
```

#### Bulk unfreeze
```bash
POST /api/v1/merchants/bulk/unfreeze
{
  "merchantIds": ["uuid-1", "uuid-2"],
  "targetStatus": "MEDIUM",
  "reason": "Investigation completed. No fraud confirmed."
}
```

#### Bulk feature enable/disable
```bash
POST /api/v1/merchants/bulk/feature/disable
{ "merchantIds": ["uuid-1"], "feature": "WITHDRAWALS" }

POST /api/v1/merchants/bulk/feature/enable
{ "merchantIds": ["uuid-1"], "feature": "WITHDRAWALS" }
```

---

### Risk Engine Endpoints

#### Evaluate merchant risk
```bash
POST /api/v1/risk/evaluate/{merchantId}

Response: {
  "merchantId": "...",
  "riskScore": 78,
  "riskLevel": "HIGH",
  "previousRiskLevel": "MEDIUM",
  "riskLevelChanged": true,
  "autoFrozen": false,
  "ruleBreakdown": [
    { "ruleName": "VELOCITY_RULE", "score": 32, "maxWeight": 40, "triggered": false,
      "reason": "80 transactions in last hour (limit: 100) → 80% of threshold" },
    { "ruleName": "AMOUNT_THRESHOLD_RULE", "score": 35, "maxWeight": 35, "triggered": true,
      "reason": "Daily volume: $520,000 (limit: $500,000, 104%)" },
    { "ruleName": "FREQUENCY_RULE", "score": 11, "maxWeight": 25, "triggered": false,
      "reason": "90 daily transactions (limit: 200) → 45% of threshold" }
  ],
  "summary": "Score: 78/100 | Level: HIGH | Rules: [AMOUNT_THRESHOLD_RULE(+35)]"
}
```

---

### Audit Log Endpoints

#### Search audit logs
```bash
GET /api/v1/audit?actionType=BULK_FREEZE&performedBy=riskops&from=2025-01-01T00:00:00&page=0&size=50

Response: { "content": [ { "id": "...", "actionType": "BULK_FREEZE", ... } ] }
```

#### Entity history (all changes to one merchant)
```bash
GET /api/v1/audit/MERCHANT/{merchantId}
```

#### CSV export (Admin only)
```bash
GET /api/v1/audit/export/csv?from=2025-01-01T00:00:00&to=2025-02-28T23:59:59
Authorization: Basic YWRtaW46YWRtaW4xMjM=
Content-Disposition: attachment; filename="audit-log-export.csv"
```

---

### Dashboard Metrics
```bash
GET /api/v1/merchants/dashboard/metrics

Response: {
  "flaggedToday": 3,
  "resolvedToday": 1,
  "pendingReviews": 4,
  "frozenMerchants": 2,
  "highRiskMerchants": 2,
  "mediumRiskMerchants": 3,
  "lowRiskMerchants": 3,
  "totalMerchants": 10
}
```

---

## Project Structure

```
src/main/java/com/fintech/riskops/
├── RiskOpsDashboardApplication.java
├── config/
│   ├── SecurityConfig.java       # Role-based auth (ANALYST/RISK_OPS/ADMIN)
│   └── AsyncConfig.java          # Thread pool for bulk operations
├── controller/
│   ├── MerchantController.java   # CRUD + bulk actions + dashboard
│   ├── RiskController.java       # Risk evaluation trigger
│   └── AuditLogController.java   # Audit search + CSV export
├── service/
│   ├── MerchantService.java      # Business logic, @Transactional
│   ├── RiskEvaluationService.java # Engine orchestration
│   └── AuditLogService.java      # Propagation.REQUIRES_NEW audit writes
├── risk/
│   ├── engine/
│   │   ├── RiskRule.java         # Strategy interface
│   │   ├── RiskEngine.java       # Orchestrator (collects all rules via DI)
│   │   ├── RiskEvaluationContext.java
│   │   ├── RiskEvaluationResult.java
│   │   └── RuleResult.java
│   └── rules/
│       ├── VelocityRule.java     # Hourly transaction rate
│       ├── AmountThresholdRule.java # Single + daily amounts
│       └── FrequencyRule.java    # Daily transaction count
├── entity/
│   ├── Merchant.java
│   ├── Transaction.java
│   └── AuditLog.java
├── repository/
│   ├── MerchantRepository.java
│   ├── TransactionRepository.java
│   └── AuditLogRepository.java
├── dto/
│   ├── request/  CreateMerchantRequest, BulkActionRequest
│   └── response/ MerchantResponse, BulkActionResponse,
│                 RiskEvaluationResponse, DashboardMetricsResponse,
│                 AuditLogResponse
├── enums/ RiskStatus, MerchantFeature, ActionType
└── exception/ MerchantNotFoundException, GlobalExceptionHandler
```

---

## Key Design Decisions

### 1. Audit Log Transaction Isolation (`Propagation.REQUIRES_NEW`)
Audit writes use a separate DB transaction from the main operation. This means if a merchant update fails and rolls back, the audit record of the *attempt* still persists — critical for compliance.

### 2. Strategy Pattern for Risk Rules
Each `RiskRule` is a Spring `@Component`. The `RiskEngine` receives all rules via constructor injection (`List<RiskRule>`). Adding a new rule requires zero changes to existing code — pure Open/Closed Principle.

### 3. Immutable Audit Log
The `AuditLog` entity has no `@UpdateTimestamp`. The `AuditLogController` exposes no update/delete endpoints. Records created = records permanent.

### 4. Bulk Operations are Atomic
Each bulk endpoint is `@Transactional`. If the 47th merchant in a 100-merchant freeze batch throws an exception, Hibernate rolls back all 47 changes. The correlation ID groups all audit records from the same job.

### 5. BigDecimal for All Monetary Values
Never `float` or `double` for money. `DECIMAL(19, 4)` in the DB; `BigDecimal` in Java. Rounding errors in financial systems have caused real-world losses.

---

## Resume Description

**Risk Operations Automation Dashboard** | Java 17 · Spring Boot · Spring Security · JPA/Hibernate · PostgreSQL

Built a production-grade internal fintech dashboard to replace Excel-based risk workflows. Implemented a configurable rule-based risk engine (Strategy Pattern) evaluating merchants across velocity, amount, and frequency dimensions. Engineered fully atomic bulk operations (freeze/unfreeze/feature toggles) with correlation-linked immutable audit trails using separate transaction propagation to ensure compliance record durability even on rollback. Features role-based access control (ANALYST/RISK_OPS/ADMIN), paginated audit log search with CSV export, and a real-time metrics dashboard.
