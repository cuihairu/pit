package io.oddsmaker.control.jpa;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

/**
 * 角色实体
 * 定义系统中的角色和权限分配
 */
@Entity
@Table(name = "roles")
public class RoleEntity {

    @Id
    @Column(length = 50)
    public String id;

    @Column(nullable = false, unique = true, length = 100)
    public String name;

    @Column(length = 500)
    public String description;

    /**
     * 角色类型: SYSTEM, CUSTOM
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    public RoleType type = RoleType.CUSTOM;

    /**
     * 角色级别: 高级别角色可以管理低级别角色
     */
    @Column(name = "level")
    public Integer level = 0;

    /**
     * 是否启用
     */
    @Column(name = "enabled")
    public Boolean enabled = true;

    /**
     * 是否系统内置角色
     */
    @Column(name = "system")
    public Boolean system = false;

    /**
     * 角色关联的权限
     */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "role_permissions",
        joinColumns = @JoinColumn(name = "role_id"),
        inverseJoinColumns = @JoinColumn(name = "permission_id")
    )
    public Set<PermissionEntity> permissions;

    // 时间戳
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    public LocalDateTime updatedAt;

    /**
     * 角色类型枚举
     */
    public enum RoleType {
        SYSTEM,  // 系统内置角色
        CUSTOM   // 自定义角色
    }

    // 业务方法
    public boolean isEnabled() {
        return enabled != null && enabled;
    }

    public boolean isSystem() {
        return system != null && system;
    }

    public boolean hasPermission(String permissionId) {
        return permissions != null && permissions.stream()
            .anyMatch(p -> p.id.equals(permissionId) && p.isEnabled());
    }

    public boolean hasPermission(String resourceType, PermissionEntity.PermissionAction action) {
        return permissions != null && permissions.stream()
            .anyMatch(p -> p.resourceType != null && 
                          p.resourceType.equals(resourceType) && 
                          p.action == action && 
                          p.isEnabled());
    }

    public Set<String> getPermissionIds() {
        if (permissions == null) {
            return Set.of();
        }
        return permissions.stream()
            .map(p -> p.id)
            .collect(java.util.stream.Collectors.toSet());
    }
}