package com.fintech.riskops.enums;

/**
 * Features that can be individually enabled/disabled per merchant.
 * This granular control allows risk teams to restrict specific capabilities
 * without fully freezing a merchant (e.g., allow viewing but not withdrawals).
 */
public enum MerchantFeature {
    PAYMENTS,
    WITHDRAWALS,
    API_ACCESS
}
