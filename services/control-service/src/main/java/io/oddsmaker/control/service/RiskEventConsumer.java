package io.oddsmaker.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oddsmaker.control.dto.RiskEventDto;
import io.oddsmaker.control.jpa.AuditLogEntity;
import io.oddsmaker.control.jpa.BlockListEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 风控事件消费者
 * 消费 Flink RiskJob 产出到 oddsmaker.risk_events topic 的 JSON 消息，
 * 按 action 分发处置：BLOCK→封禁名单、WEBHOOK→Webhook 通知、ALERT/REVIEW/THROTTLE→审计日志。
 */
@Component
public class RiskEventConsumer {

    private static final Logger logger = LoggerFactory.getLogger(RiskEventConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private BlockListService blockListService;

    @Autowired
    private WebhookService webhookService;

    @Autowired
    private AuditLogService auditLogService;

    @KafkaListener(topics = "oddsmaker.risk_events")
    public void onRiskEvent(String message) {
        RiskEventDto event;
        try {
            event = objectMapper.readValue(message, RiskEventDto.class);
        } catch (Exception e) {
            logger.error("Failed to deserialize risk event: {}", e.getMessage());
            return;
        }

        if (event.action == null || event.action.isBlank()) {
            logger.warn("Risk event {} has no action, skipping", event.riskEventId);
            return;
        }

        logger.info("Processing risk event: action={}, ruleId={}, subject={}:{}, score={}",
            event.action, event.ruleId, event.subjectType, event.subjectId, event.score);

        try {
            switch (event.action.toUpperCase()) {
                case "BLOCK":
                    handleBlock(event);
                    break;
                case "WEBHOOK":
                    handleWebhook(event);
                    break;
                case "ALERT":
                case "REVIEW":
                case "THROTTLE":
                    handleAuditOnly(event);
                    break;
                default:
                    logger.warn("Unknown risk action: {}, logging as SECURITY_ALERT", event.action);
                    handleAuditOnly(event);
                    break;
            }
        } catch (Exception e) {
            logger.error("Failed to handle risk event {}: {}", event.riskEventId, e.getMessage(), e);
        }
    }

    // ========== 处置方法 ==========

    private void handleBlock(RiskEventDto event) {
        // subject_type → targetType 映射
        String targetType = switch (event.subjectType != null ? event.subjectType.toUpperCase() : "") {
            case "PLAYER" -> "player_id";
            case "DEVICE" -> "device_id";
            default -> event.subjectType != null ? event.subjectType.toLowerCase() : "unknown";
        };

        // severity → 时长推导
        boolean isPermanent = false;
        Integer durationMinutes = null;
        if (event.severity != null) {
            switch (event.severity.toUpperCase()) {
                case "CRITICAL":
                    isPermanent = true;
                    break;
                case "HIGH":
                    durationMinutes = 1440;   // 1 day
                    break;
                case "MEDIUM":
                    durationMinutes = 240;     // 4 hours
                    break;
                case "LOW":
                    durationMinutes = 60;      // 1 hour
                    break;
                default:
                    durationMinutes = 1440;
                    break;
            }
        }

        blockListService.addBlock(
            event.gameId,
            event.environment,
            targetType,
            event.subjectId,
            event.reason != null ? event.reason : "Risk automation: " + event.riskType,
            "fraud",
            BlockListEntity.BlockType.HARD,
            isPermanent,
            durationMinutes,
            "risk-automation",
            null,
            event.reason
        );

        logger.info("BLOCK applied: {}={} in game={}, permanent={}, duration={}min",
            targetType, event.subjectId, event.gameId, isPermanent, durationMinutes);
    }

    private void handleWebhook(RiskEventDto event) {
        Map<String, Object> payload = buildWebhookPayload(event);
        webhookService.sendCustomWebhook(event.gameId, "risk_event", payload);
        logger.info("WEBHOOK sent for risk event {}", event.riskEventId);
    }

    private void handleAuditOnly(RiskEventDto event) {
        auditLogService.log(
            AuditLogEntity.AuditAction.SECURITY_ALERT,
            "risk_event",
            event.riskEventId,
            event.riskType != null ? event.riskType : "unknown",
            event.reason,
            AuditLogEntity.AuditResult.SUCCESS,
            "risk-automation",
            null,
            null,
            null,
            null,
            Map.of(
                "gameId", event.gameId != null ? event.gameId : "",
                "environment", event.environment != null ? event.environment : "",
                "action", event.action != null ? event.action : "",
                "subjectType", event.subjectType != null ? event.subjectType : "",
                "subjectId", event.subjectId != null ? event.subjectId : "",
                "ruleId", event.ruleId != null ? event.ruleId : "",
                "score", String.valueOf(event.score)
            )
        );

        logger.info("AUDIT logged for risk event {} (action={})", event.riskEventId, event.action);
    }

    // ========== 辅助方法 ==========

    private Map<String, Object> buildWebhookPayload(RiskEventDto event) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("event_type", "risk_event");
        payload.put("game_id", event.gameId);
        payload.put("environment", event.environment);
        payload.put("ts", event.ts);
        payload.put("risk_event_id", event.riskEventId);
        payload.put("source_event_id", event.sourceEventId);
        payload.put("rule_id", event.ruleId);
        payload.put("risk_type", event.riskType);
        payload.put("severity", event.severity);
        payload.put("subject_type", event.subjectType);
        payload.put("subject_id", event.subjectId);
        payload.put("score", event.score);
        payload.put("action", event.action);
        payload.put("reason", event.reason);
        payload.put("evidence", event.evidence);
        return payload;
    }
}
