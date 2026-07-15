package io.oddsmaker.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oddsmaker.control.dto.RiskEventDto;
import io.oddsmaker.control.jpa.AuditLogEntity;
import io.oddsmaker.control.jpa.BlockListEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 风控事件消费者处置测试。
 *
 * 替代无法在无 docker 环境下执行的 Step 5 完整端到端验证：
 * 用真实 ObjectMapper 反序列化对齐 RiskJob.toJson 契约（snake_case）的 JSON，
 * 验证消费者按 action 正确分发处置（BLOCK→addBlock / WEBHOOK→sendCustomWebhook /
 * ALERT·REVIEW·THROTTLE→审计），并校验 BLOCK 处置矩阵
 * （subject_type→targetType、severity→duration/permanent）。
 *
 * 这条测试直接覆盖 Phase 0.1 的核心断点修复："risk_events 有了消费者" +
 * "BLOCK 真正落到封禁名单 + action 正确分发"。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("风控事件消费者处置测试")
class RiskEventConsumerTest {

    @Mock private BlockListService blockListService;
    @Mock private WebhookService webhookService;
    @Mock private AuditLogService auditLogService;

    private RiskEventConsumer consumer;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consumer = new RiskEventConsumer();
        // RiskEventConsumer 用字段注入，单元测试下手动注入
        ReflectionTestUtils.setField(consumer, "objectMapper", om);
        ReflectionTestUtils.setField(consumer, "blockListService", blockListService);
        ReflectionTestUtils.setField(consumer, "webhookService", webhookService);
        ReflectionTestUtils.setField(consumer, "auditLogService", auditLogService);
    }

    /** 构造对齐 RiskJob.toJson 输出契约（snake_case）的风控事件 JSON */
    private String riskEventJson(String action, String severity, String subjectType, String subjectId, String ruleId) {
        return "{"
            + "\"game_id\":\"game_demo\""
            + ",\"environment\":\"prod\""
            + ",\"ts\":1730000000000"
            + ",\"risk_event_id\":\"re_001\""
            + ",\"source_event_id\":\"evt_001\""
            + ",\"rule_id\":\"" + ruleId + "\""
            + ",\"risk_type\":\"amount_threshold\""
            + ",\"severity\":\"" + severity + "\""
            + ",\"subject_type\":\"" + subjectType + "\""
            + ",\"subject_id\":\"" + subjectId + "\""
            + ",\"score\":0.95"
            + ",\"action\":\"" + action + "\""
            + ",\"reason\":\"Amount exceeds threshold\""
            + ",\"evidence\":{\"amount\":\"999\",\"threshold\":\"100\"}"
            + "}";
    }

    @Test
    @DisplayName("BLOCK + DEVICE + HIGH → 写封禁名单：封 device_id，时长 1440 分钟，非永久")
    void blockDeviceHigh_appliesBlock() {
        consumer.onRiskEvent(riskEventJson("BLOCK", "HIGH", "DEVICE", "dev_abc", "rr_threshold"));

        verify(blockListService).addBlock(
            eq("game_demo"), eq("prod"),
            eq("device_id"), eq("dev_abc"),
            eq("Amount exceeds threshold"),
            eq("fraud"), eq(BlockListEntity.BlockType.HARD),
            eq(false), eq(1440),
            eq("risk-automation"), isNull(), eq("Amount exceeds threshold"));
        verifyNoInteractions(webhookService, auditLogService);
    }

    @Test
    @DisplayName("BLOCK + CRITICAL → 永久封禁（durationMinutes=null）")
    void blockCritical_isPermanent() {
        consumer.onRiskEvent(riskEventJson("BLOCK", "CRITICAL", "DEVICE", "dev_perm", "rr_threshold"));

        verify(blockListService).addBlock(
            eq("game_demo"), eq("prod"), eq("device_id"), eq("dev_perm"),
            anyString(), eq("fraud"), eq(BlockListEntity.BlockType.HARD),
            eq(true), isNull(),
            eq("risk-automation"), isNull(), anyString());
    }

    @Test
    @DisplayName("BLOCK + PLAYER → subject_type 映射为 player_id")
    void blockPlayer_mapsToPlayerId() {
        consumer.onRiskEvent(riskEventJson("BLOCK", "MEDIUM", "PLAYER", "user_42", "rr_freq"));

        verify(blockListService).addBlock(
            eq("game_demo"), eq("prod"),
            eq("player_id"), eq("user_42"),
            anyString(), eq("fraud"), eq(BlockListEntity.BlockType.HARD),
            eq(false), eq(240),
            eq("risk-automation"), isNull(), anyString());
    }

    @Test
    @DisplayName("ALERT → 仅记 SECURITY_ALERT 审计，不封禁")
    void alert_logsAuditOnly() {
        consumer.onRiskEvent(riskEventJson("ALERT", "LOW", "DEVICE", "dev_alert", "rr_vel"));

        // addBlock 全 matcher：第 8 参数是 primitive boolean，必须用 anyBoolean()
        verify(blockListService, never()).addBlock(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean(),
            any(), any(), any(), any());
        verify(auditLogService, times(1)).log(
            eq(AuditLogEntity.AuditAction.SECURITY_ALERT), any(), any(),
            any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("WEBHOOK → 发送自定义 Webhook，不封禁")
    void webhook_sendsCustomWebhook() {
        consumer.onRiskEvent(riskEventJson("WEBHOOK", "HIGH", "DEVICE", "dev_wh", "rr_webhook"));

        verify(webhookService).sendCustomWebhook(eq("game_demo"), eq("risk_event"), anyMap());
        verify(blockListService, never()).addBlock(
            any(), any(), any(), any(), any(), any(), any(), anyBoolean(),
            any(), any(), any(), any());
    }

    @Test
    @DisplayName("非法 JSON → 静默跳过，不抛异常、不触发任何下游处置")
    void invalidJson_skipsGracefully() {
        assertDoesNotThrow(() -> consumer.onRiskEvent("{not a valid json"));
        verifyNoInteractions(blockListService, webhookService, auditLogService);
    }

    @Test
    @DisplayName("JSON 契约对齐 RiskJob.toJson（snake_case 全字段正确反序列化）")
    void jsonContract_alignedWithRiskJob() throws Exception {
        RiskEventDto dto = om.readValue(
            riskEventJson("BLOCK", "HIGH", "DEVICE", "dev_abc", "rr_threshold"),
            RiskEventDto.class);

        assertEquals("game_demo", dto.gameId);
        assertEquals("prod", dto.environment);
        assertEquals(1730000000000L, dto.ts);
        assertEquals("re_001", dto.riskEventId);
        assertEquals("evt_001", dto.sourceEventId);
        assertEquals("rr_threshold", dto.ruleId);
        assertEquals("amount_threshold", dto.riskType);
        assertEquals("HIGH", dto.severity);
        assertEquals("DEVICE", dto.subjectType);
        assertEquals("dev_abc", dto.subjectId);
        assertEquals(0.95f, dto.score, 0.001f);
        assertEquals("BLOCK", dto.action);
        assertEquals("Amount exceeds threshold", dto.reason);
        assertNotNull(dto.evidence);
        assertEquals("999", dto.evidence.get("amount"));
    }
}
