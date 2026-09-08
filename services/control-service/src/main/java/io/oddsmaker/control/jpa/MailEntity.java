package io.oddsmaker.control.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 运营邮件：全服（ALL）或个人（INDIVIDUAL），带附件与过期时间。
 * 全服邮件惰性展开：玩家拉取收件箱时判定可见，领取记录去重。
 */
@Entity
@Table(name = "op_mails",
    indexes = {
        @Index(name = "idx_mail_game", columnList = "game_id"),
        @Index(name = "idx_mail_status", columnList = "game_id, status")
    })
public class MailEntity {

    @Id
    @Column(length = 32)
    public String id;

    @Column(name = "game_id", nullable = false, length = 32)
    public String gameId;

    /** 环境ID；null = 全环境 */
    @Column(name = "environment_id", length = 64)
    public String environmentId;

    @Column(nullable = false, length = 200)
    public String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String content;

    /** 发送范围 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Scope scope = Scope.ALL;

    /** 个人邮件收件人（player_id/user_id 逗号分隔）；scope=INDIVIDUAL 时必填 */
    @Column(length = 4000)
    public String recipients;

    /** 附件 JSON 数组：[{"type":"item","id":"gem","count":100}] */
    @Column(name = "attachments", columnDefinition = "TEXT")
    public String attachments;

    /** 领取截止时间；过期后玩家不可领取（仍可在管理端查看） */
    @Column(name = "expire_at")
    public LocalDateTime expireAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Status status = Status.DRAFT;

    @Column(name = "sent_at")
    public LocalDateTime sentAt;

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

    public enum Scope {
        ALL,         // 全服
        INDIVIDUAL   // 指定玩家
    }

    public enum Status {
        DRAFT,   // 草稿
        SENT,    // 已发送
        EXPIRED  // 已过期（sweep 标记）
    }

    public boolean claimable(LocalDateTime now) {
        return status == Status.SENT && deletedAt == null
            && (expireAt == null || now.isBefore(expireAt));
    }

    /** 玩家是否可见：已发送、未过期、范围匹配 */
    public boolean visibleTo(String playerKey, LocalDateTime now) {
        if (!claimable(now)) return false;
        if (scope == Scope.ALL) return true;
        return recipients != null && containsRecipient(recipients, playerKey);
    }

    public static boolean containsRecipient(String recipients, String playerKey) {
        for (String r : recipients.split(",")) {
            if (r.trim().equals(playerKey)) return true;
        }
        return false;
    }
}
