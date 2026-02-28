package com.fintech.riskops.enums;

/**
 * Risk status of a merchant.
 * Progression: LOW -> MEDIUM -> HIGH -> FROZEN
 * FROZEN merchants are fully locked - no transactions possible.
 */
public enum RiskStatus {
    LOW,
    MEDIUM,
    HIGH,
    FROZEN
}
