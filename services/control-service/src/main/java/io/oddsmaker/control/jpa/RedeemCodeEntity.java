package io.oddsmaker.control.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 兑换码：UNIQUE 批次每码一条（一次性）；SHARED 批次单条（通用码，兑换由记录表控制）。
 */
@Entity
@Table(name = "redeem_codes",
    uniqueConstraints = @UniqueConstraint(name = "uk_code", columnNames = "code"),
    indexes = @Index(name = "idx_rc_batch", columnList = "batch_id"))
public class RedeemCodeEntity {

    @Id
    @Column(length = 40)
    public String id;

    @Column(name = "batch_id", nullable = false, length = 32)
    public String batchId;

    @Column(nullable = false, length = 32)
    public String code;

    /** AVAILABLE / REDEEMED（仅 UNIQUE 有意义） */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    public Status status = Status.AVAILABLE;

    @Column(name = "redeemed_by", length = 128)
    public String redeemedBy;

    @Column(name = "redeemed_at")
    public LocalDateTime redeemedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    public enum Status {
        AVAILABLE,
        REDEEMED
    }
}
