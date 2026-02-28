package com.fintech.riskops.risk;

import com.fintech.riskops.entity.Merchant;
import com.fintech.riskops.enums.RiskStatus;
import com.fintech.riskops.risk.engine.*;
import com.fintech.riskops.risk.rules.AmountThresholdRule;
import com.fintech.riskops.risk.rules.FrequencyRule;
import com.fintech.riskops.risk.rules.VelocityRule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the risk engine.
 * Tests each rule independently and then the full engine composition.
 */
class RiskEngineTest {

    private VelocityRule velocityRule;
    private AmountThresholdRule amountThresholdRule;
    private FrequencyRule frequencyRule;
    private RiskEngine riskEngine;

    @BeforeEach
    void setUp() {
        velocityRule = new VelocityRule();
        ReflectionTestUtils.setField(velocityRule, "maxTransactionsPerHour", 100L);
        ReflectionTestUtils.setField(velocityRule, "maxWeight", 40);

        amountThresholdRule = new AmountThresholdRule();
        ReflectionTestUtils.setField(amountThresholdRule, "maxSingleTransaction", new BigDecimal("50000.00"));
        ReflectionTestUtils.setField(amountThresholdRule, "maxDailyVolume", new BigDecimal("500000.00"));
        ReflectionTestUtils.setField(amountThresholdRule, "maxWeight", 35);

        frequencyRule = new FrequencyRule();
        ReflectionTestUtils.setField(frequencyRule, "maxDailyTransactions", 200L);
        ReflectionTestUtils.setField(frequencyRule, "maxWeight", 25);

        riskEngine = new RiskEngine(List.of(velocityRule, amountThresholdRule, frequencyRule));
        ReflectionTestUtils.setField(riskEngine, "mediumRiskThreshold", 40);
        ReflectionTestUtils.setField(riskEngine, "highRiskThreshold", 70);
        ReflectionTestUtils.setField(riskEngine, "autoFreezeThreshold", 90);
    }

    // ---- VelocityRule tests ----

    @Test
    @DisplayName("VelocityRule: zero transactions scores zero")
    void velocityRule_zeroTransactions_scoresZero() {
        RiskEvaluationContext ctx = buildContext(0, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        RuleResult result = velocityRule.evaluate(ctx);

        assertThat(result.getScore()).isEqualTo(0);
        assertThat(result.isTriggered()).isFalse();
    }

    @Test
    @DisplayName("VelocityRule: 50% of limit scores 50% of weight")
    void velocityRule_halfLimit_scoresHalfWeight() {
        RiskEvaluationContext ctx = buildContext(50, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        RuleResult result = velocityRule.evaluate(ctx);

        assertThat(result.getScore()).isEqualTo(20); // 50% of 40
        assertThat(result.isTriggered()).isFalse();
    }

    @Test
    @DisplayName("VelocityRule: at limit triggers with full weight")
    void velocityRule_atLimit_triggersWithFullWeight() {
        RiskEvaluationContext ctx = buildContext(100, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        RuleResult result = velocityRule.evaluate(ctx);

        assertThat(result.getScore()).isEqualTo(40);
        assertThat(result.isTriggered()).isTrue();
    }

    @Test
    @DisplayName("VelocityRule: over limit caps at max weight")
    void velocityRule_overLimit_capsAtMaxWeight() {
        RiskEvaluationContext ctx = buildContext(500, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        RuleResult result = velocityRule.evaluate(ctx);

        assertThat(result.getScore()).isEqualTo(40); // Capped
        assertThat(result.isTriggered()).isTrue();
    }

    // ---- AmountThresholdRule tests ----

    @Test
    @DisplayName("AmountThresholdRule: single txn over limit triggers")
    void amountRule_singleTxnOverLimit_triggers() {
        RiskEvaluationContext ctx = buildContext(5, 10,
            new BigDecimal("100000.00"), new BigDecimal("60000.00")); // 60k > 50k limit
        RuleResult result = amountThresholdRule.evaluate(ctx);

        assertThat(result.isTriggered()).isTrue();
        assertThat(result.getScore()).isGreaterThan(0);
    }

    @Test
    @DisplayName("AmountThresholdRule: normal amounts score zero")
    void amountRule_normalAmounts_scoresZero() {
        RiskEvaluationContext ctx = buildContext(5, 10,
            new BigDecimal("1000.00"), new BigDecimal("500.00"));
        RuleResult result = amountThresholdRule.evaluate(ctx);

        assertThat(result.isTriggered()).isFalse();
        assertThat(result.getScore()).isLessThan(5); // Very low score
    }

    // ---- Full engine tests ----

    @Test
    @DisplayName("RiskEngine: clean merchant scores LOW")
    void engine_cleanMerchant_scoresLow() {
        RiskEvaluationContext ctx = buildContext(5, 10,
            new BigDecimal("500.00"), new BigDecimal("200.00"));
        RiskEvaluationResult result = riskEngine.evaluate(ctx);

        assertThat(result.getRiskLevel()).isEqualTo(RiskStatus.LOW);
        assertThat(result.isShouldAutoFreeze()).isFalse();
        assertThat(result.getRuleResults()).hasSize(3);
    }

    @Test
    @DisplayName("RiskEngine: high velocity triggers MEDIUM risk")
    void engine_highVelocity_triggersMediumRisk() {
        // 60% of velocity limit = 24 points, other rules near zero = ~24 total = MEDIUM (>=40? No, MEDIUM is 40)
        // Use 80% of velocity (32 pts) + some frequency (10 pts) = 42 = MEDIUM
        RiskEvaluationContext ctx = buildContext(80, 80,
            new BigDecimal("1000.00"), new BigDecimal("500.00"));
        RiskEvaluationResult result = riskEngine.evaluate(ctx);

        assertThat(result.getTotalScore()).isGreaterThanOrEqualTo(40);
        assertThat(result.getRiskLevel()).isIn(RiskStatus.MEDIUM, RiskStatus.HIGH);
    }

    @Test
    @DisplayName("RiskEngine: all rules maxed triggers auto-freeze")
    void engine_allRulesMaxed_triggersAutoFreeze() {
        RiskEvaluationContext ctx = buildContext(
            200,      // 2x velocity limit
            400,      // 2x daily txn limit
            new BigDecimal("600000.00"),  // over daily volume limit
            new BigDecimal("60000.00")    // over single txn limit
        );
        RiskEvaluationResult result = riskEngine.evaluate(ctx);

        assertThat(result.getTotalScore()).isEqualTo(100); // Capped at 100
        assertThat(result.isShouldAutoFreeze()).isTrue();
        assertThat(result.getRiskLevel()).isEqualTo(RiskStatus.HIGH);
    }

    @Test
    @DisplayName("RiskEngine: rule breakdown contains all 3 rules")
    void engine_returnsBreakdownForAllRules() {
        RiskEvaluationContext ctx = buildContext(10, 20,
            new BigDecimal("500.00"), new BigDecimal("100.00"));
        RiskEvaluationResult result = riskEngine.evaluate(ctx);

        assertThat(result.getRuleResults())
            .extracting(RuleResult::getRuleName)
            .containsExactlyInAnyOrder("VELOCITY_RULE", "AMOUNT_THRESHOLD_RULE", "FREQUENCY_RULE");
    }

    // ---- Merchant domain tests ----

    @Test
    @DisplayName("Merchant.freeze() clears all features")
    void merchant_freeze_clearsFeatures() {
        Merchant merchant = Merchant.builder()
            .id(UUID.randomUUID())
            .name("Test")
            .email("test@test.com")
            .riskStatus(RiskStatus.HIGH)
            .build();
        merchant.getEnabledFeatures().add(com.fintech.riskops.enums.MerchantFeature.PAYMENTS);
        merchant.getEnabledFeatures().add(com.fintech.riskops.enums.MerchantFeature.WITHDRAWALS);

        merchant.freeze();

        assertThat(merchant.getRiskStatus()).isEqualTo(RiskStatus.FROZEN);
        assertThat(merchant.getEnabledFeatures()).isEmpty();
    }

    // ---- Helper ----

    private RiskEvaluationContext buildContext(
        long txnsLastHour, long txnsToday,
        BigDecimal dailyVolume, BigDecimal maxSingleTxn
    ) {
        Merchant merchant = Merchant.builder()
            .id(UUID.randomUUID())
            .name("Test Merchant")
            .email("test@merchant.com")
            .riskStatus(RiskStatus.LOW)
            .riskScore(0)
            .build();

        return RiskEvaluationContext.builder()
            .merchant(merchant)
            .transactionsLastHour(txnsLastHour)
            .transactionsToday(txnsToday)
            .dailyVolume(dailyVolume)
            .maxSingleTransactionToday(maxSingleTxn)
            .build();
    }
}
