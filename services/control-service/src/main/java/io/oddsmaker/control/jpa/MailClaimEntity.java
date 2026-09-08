package io.oddsmaker.control.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 邮件领取记录：幂等去重（mailId + playerKey 唯一），同时作为附件发放凭据。
 */
@Entity
@Table(name = "op_mail_claims",
    uniqueConstraints = @UniqueConstraint(name = "uk_claim_mail_player", columnNames = {"mail_id", "player_key"}),
    indexes = @Index(name = "idx_claim_player", columnList = "game_id, player_key"))
public class MailClaimEntity {

    @Id
    @Column(length = 48)
    public String id;

    @Column(name = "mail_id", nullable = false, length = 32)
    public String mailId;

    @Column(name = "game_id", nullable = false, length = 32)
    public String gameId;

    /** 玩家标识（player_id 优先，退化 user_id） */
    @Column(name = "player_key", nullable = false, length = 128)
    public String playerKey;

    /** 附件快照（发放内容固化，后续邮件编辑不影响已发放） */
    @Column(name = "claimed_attachments", columnDefinition = "TEXT")
    public String claimedAttachments;

    @Column(name = "claimed_at", nullable = false)
    public LocalDateTime claimedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;
}
