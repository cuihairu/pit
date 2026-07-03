package io.oddsmaker.control.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 漏斗配置实体
 * 定义漏斗分析的步骤和配置
 */
@Entity
@Table(name = "funnel_configs")
public class FunnelConfigEntity {

    @Id
    @Column(length = 32)
    public String id;

    @Column(name = "game_id", nullable = false, length = 32)
    public String gameId;

    @Column(nullable = false, length = 100)
    public String name;

    @Column(length = 500)
    public String description;

    /**
     * 漏斗类型: standard, sequential, time_window
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    public FunnelType type = FunnelType.STANDARD;

    /**
     * 用户标识字段: user_id, device_id, player_id
     */
    @Column(name = "user_key", nullable = false, length = 50)
    public String userKey = "user_id";

    /**
     * 时间窗口（秒），用于time_window类型
     */
    @Column(name = "time_window_sec")
    public Long timeWindowSec;

    /**
     * 是否启用
     */
    @Column(name = "enabled")
    public Boolean enabled = true;

    /**
     * 漏斗步骤
     */
    @OneToMany(mappedBy = "funnel", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("stepOrder ASC")
    public List<FunnelStepEntity> steps;

    // 时间戳
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    public LocalDateTime deletedAt;

    /**
     * 漏斗类型枚举
     */
    public enum FunnelType {
        STANDARD,      // 标准漏斗（任意顺序）
        SEQUENTIAL,    // 顺序漏斗（必须按顺序完成）
        TIME_WINDOW    // 时间窗口漏斗（在指定时间内完成）
    }

    // 业务方法
    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public boolean isSequential() {
        return type == FunnelType.SEQUENTIAL;
    }

    public boolean isTimeWindow() {
        return type == FunnelType.TIME_WINDOW;
    }

    public int getStepCount() {
        return steps != null ? steps.size() : 0;
    }
}