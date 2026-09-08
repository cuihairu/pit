package io.oddsmaker.control.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 兑换码批次：定义奖励、限领、有效期与总量。
 * UNIQUE 批次批量生成一次性码；SHARED 批次单一通用码（全服可兑，每人限领）。
 */
@Entity
@Table(name = "redeem_batches",
    indexes = @Index(name = "idx_rb_game", columnList = "game_id"))
public class RedeemCodeBatchEntity {

    @Id
    @Column(length = 32)
    public String id;

    @Column(name = "game_id", nullable = false, length = 32)
    public String gameId;

    /** 环境ID；null = 全环境 */
    @Column(name = "environment_id", length = 64)
    public String environmentId;

    @Column(nullable = false, length = 100)
    public String name;

    /** 奖励 JSON 数组（与邮件附件同构）：[{"type":"item","id":"gem","count":100}] */
    @Column(nullable = false, columnDefinition = "TEXT")
    public String reward;

    /** 批次类型：UNIQUE 唯一码 / SHARED 通用码 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public CodeType codeType = CodeType.UNIQUE;

    /** UNIQUE：生成码数量；SHARED：总兑换次数上限（0 = 不限） */
    @Column(name = "total", nullable = false)
    public int total;

    /** 每玩家限领次数（防刷核心），默认 1 */
    @Column(name = "per_user_limit", nullable = false)
    public int perUserLimit = 1;

    /** 兑换截止时间；null = 永不过期 */
    @Column(name = "expires_at")
    public LocalDateTime expiresAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Status status = Status.ACTIVE;

    @Column(name = "created_by", length = 64)
    public String createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    public LocalDateTime deletedAt;

    public enum CodeType {
        UNIQUE,   // 一次性唯一码
        SHARED    // 通用码
    }

    public enum Status {
        ACTIVE,
        DISABLED
    }

    public boolean redeemable(LocalDateTime now) {
        return status == Status.ACTIVE && deletedAt == null
            && (expiresAt == null || now.isBefore(expiresAt));
    }
}
