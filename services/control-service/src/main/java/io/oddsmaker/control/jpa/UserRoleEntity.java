package io.oddsmaker.control.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 用户-角色关联实体
 * 存储用户和角色的多对多关系
 */
@Entity
@Table(name = "user_role_assignments")
public class UserRoleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    /**
     * 用户ID
     */
    @Column(name = "user_id", nullable = false, length = 64)
    public String userId;

    /**
     * 角色ID
     */
    @Column(name = "role_id", nullable = false, length = 50)
    public String roleId;

    /**
     * 游戏ID（如果角色是游戏级）
     */
    @Column(name = "game_id", length = 32)
    public String gameId;

    /**
     * 环境（如果角色是环境级）
     */
    @Column(name = "environment", length = 20)
    public String environment;

    /**
     * 分配者ID
     */
    @Column(name = "assigned_by", length = 64)
    public String assignedBy;

    /**
     * 分配时间
     */
    @CreationTimestamp
    @Column(name = "assigned_at", nullable = false)
    public LocalDateTime assignedAt;

    /**
     * 过期时间
     */
    @Column(name = "expires_at")
    public LocalDateTime expiresAt;

    /**
     * 是否启用
     */
    @Column(name = "enabled")
    public Boolean enabled = true;

    // 业务方法
    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return isEnabled() && !isExpired();
    }

    public boolean isGlobal() {
        return gameId == null;
    }

    public boolean isGameScoped() {
        return gameId != null && environment == null;
    }

    public boolean isEnvironmentScoped() {
        return gameId != null && environment != null;
    }

    /**
     * 角色类型枚举
     */
    public enum RoleType {
        PLAYER,
        ADMIN,
        ANALYST,
        DEVELOPER,
        VIEWER
    }

    /**
     * 权限范围枚举
     */
    public enum PermissionScope {
        GLOBAL,
        GAME,
        ENVIRONMENT
    }
}