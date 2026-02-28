package com.fintech.riskops.enums;

/**
 * Immutable catalog of all auditable actions in the system.
 * Adding a new action type here (and logging it) ensures nothing
 * slips through the compliance audit trail.
 */
public enum ActionType {
    // Merchant lifecycle
    MERCHANT_CREATED,
    MERCHANT_UPDATED,
    MERCHANT_DELETED,

    // Risk status changes
    MERCHANT_FROZEN,
    MERCHANT_UNFROZEN,
    RISK_STATUS_CHANGED,

    // Feature toggles
    FEATURE_ENABLED,
    FEATURE_DISABLED,

    // Bulk operations (separate types for easy filtering)
    BULK_FREEZE,
    BULK_UNFREEZE,
    BULK_FEATURE_ENABLE,
    BULK_FEATURE_DISABLE,

    // Risk engine
    RISK_EVALUATED,
    RISK_AUTO_FLAGGED,
    RISK_AUTO_FROZEN,

    // Transaction events
    TRANSACTION_CREATED,
    TRANSACTION_FLAGGED
}
