package io.oddsmaker.control.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 兑换记录：奖励发放凭据 + 防刷计数依据。
 * (batchId, playerKey, seq) 唯一，seq 为该玩家在此批次的第几次兑换。
 */
@Entity
@Table(name = "redeem_records",
    uniqueConstraints = @UniqueConstraint(name = "uk_rr_batch_player_seq",
        columnNames = {"batch_id", "player_key", "seq"}),
    indexes = {
        @Index(name = "idx_rr_batch", columnList = "batch_id"),
        @Index(name = "idx_rr_player", columnList = "game_id, player_key")
    })
public class RedeemRecordEntity {

    @Id
    @Column(length = 48)
    public String id;

    @Column(name = "batch_id", nullable = false, length = 32)
    public String batchId;

    @Column(name = "game_id", nullable = false, length = 32)
    public String gameId;

    @Column(name = "player_key", nullable = false, length = 128)
    public String playerKey;

    /** 同一批次内该玩家的第几次兑换（从 1 开始） */
    @Column(nullable = false)
    public int seq;

    @Column(nullable = false, length = 32)
    public String code;

    /** 奖励快照（发放内容固化） */
    @Column(nullable = false, columnDefinition = "TEXT")
    public String reward;

    @Column(name = "redeemed_at", nullable = false)
    public LocalDateTime redeemedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
}
