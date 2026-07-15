package io.oddsmaker.control.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

/**
 * 风控事件 DTO，对齐 RiskJob.toJson 输出的 JSON 字段（snake_case）
 */
public class RiskEventDto {

    @JsonProperty("game_id")
    public String gameId;

    @JsonProperty("environment")
    public String environment;

    @JsonProperty("ts")
    public Long ts;

    @JsonProperty("risk_event_id")
    public String riskEventId;

    @JsonProperty("source_event_id")
    public String sourceEventId;

    @JsonProperty("rule_id")
    public String ruleId;

    @JsonProperty("risk_type")
    public String riskType;

    @JsonProperty("severity")
    public String severity;

    @JsonProperty("subject_type")
    public String subjectType;   // PLAYER / DEVICE

    @JsonProperty("subject_id")
    public String subjectId;

    @JsonProperty("score")
    public Float score;

    @JsonProperty("action")
    public String action;        // BLOCK / ALERT / WEBHOOK / REVIEW / THROTTLE

    @JsonProperty("reason")
    public String reason;

    @JsonProperty("evidence")
    public Map<String, String> evidence;
}
