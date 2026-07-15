package io.oddsmaker.jobs.risk;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class RiskJobTest {

    @Test
    void strReturnsNullForNull() {
        String result = RiskJob.str(null);
        assertNull(result);
    }

    @Test
    void strReturnsStringForNonNull() {
        String result = RiskJob.str("test");
        assertEquals("test", result);
    }

    @Test
    void nzReturnsEmptyStringForNull() {
        String result = RiskJob.nz(null);
        assertEquals("", result);
    }

    @Test
    void nzReturnsStringForNonNull() {
        String result = RiskJob.nz("test");
        assertEquals("test", result);
    }

    @Test
    void parseAmountReturnsNullForNull() {
        BigDecimal result = RiskJob.parseAmount(null);
        assertNull(result);
    }

    @Test
    void parseAmountReturnsBigDecimalForNumber() {
        BigDecimal result = RiskJob.parseAmount(123.45);
        assertNotNull(result);
        assertEquals(new BigDecimal("123.45"), result);
    }

    @Test
    void parseAmountReturnsBigDecimalForString() {
        BigDecimal result = RiskJob.parseAmount("123.45");
        assertNotNull(result);
        assertEquals(new BigDecimal("123.45"), result);
    }

    @Test
    void parseAmountReturnsNullForInvalidString() {
        BigDecimal result = RiskJob.parseAmount("not_a_number");
        assertNull(result);
    }

    @Test
    void riskInputConstructor() {
        RiskJob.RiskInput input = new RiskJob.RiskInput();
        input.gameId = "game_demo";
        input.environment = "prod";
        input.eventId = "event_123";
        input.eventName = "purchase";
        input.userId = "user_456";
        input.deviceId = "device_789";
        input.amount = new BigDecimal("99.99");
        
        assertEquals("game_demo", input.gameId);
        assertEquals("prod", input.environment);
        assertEquals("event_123", input.eventId);
        assertEquals("purchase", input.eventName);
        assertEquals("user_456", input.userId);
        assertEquals("device_789", input.deviceId);
        assertEquals(new BigDecimal("99.99"), input.amount);
    }

    @Test
    void riskHitConstructor() {
        RiskJob.RiskHit hit = new RiskJob.RiskHit();
        hit.gameId = "game_demo";
        hit.environment = "prod";
        hit.subjectId = "user_456";
        hit.riskType = "amount_threshold";
        hit.score = 0.95f;
        hit.reason = "Amount exceeds threshold";
        
        assertEquals("game_demo", hit.gameId);
        assertEquals("prod", hit.environment);
        assertEquals("user_456", hit.subjectId);
        assertEquals("amount_threshold", hit.riskType);
        assertEquals(0.95f, hit.score, 0.0f);
        assertEquals("Amount exceeds threshold", hit.reason);
    }
}