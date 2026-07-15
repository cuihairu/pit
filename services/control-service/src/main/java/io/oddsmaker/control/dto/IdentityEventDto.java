package io.oddsmaker.control.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * 身份事件 DTO，对齐 IdentityMergeJob.toJson 输出的 JSON 字段（snake_case）。
 * 消息由 Flink IdentityMergeJob 双写到 oddsmaker.identity_events，control 端 IdentityConsumer 消费。
 */
public class IdentityEventDto {

    @JsonProperty("game_id")
    public String gameId;

    @JsonProperty("environment")
    public String environment;          // 环境名（如 prod），消费侧解析成 environment_id

    @JsonProperty("identity_id")
    public String identityId;           // 32 字符，对齐 PG identities.id

    @JsonProperty("user_id")
    public String userId;

    @JsonProperty("player_id")
    public String playerId;             // 主值（playerIds 首元素）

    @JsonProperty("device_ids")
    public List<String> deviceIds;

    @JsonProperty("player_ids")
    public List<String> playerIds;

    @JsonProperty("character_ids")
    public List<String> characterIds;

    @JsonProperty("first_seen")
    public Long firstSeen;              // epoch millis

    @JsonProperty("last_seen")
    public Long lastSeen;               // epoch millis
}
