package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 权限数据访问接口
 */
@Repository
public interface PermissionRepo extends JpaRepository<PermissionEntity, String> {

    /**
     * 根据名称查找权限
     */
    Optional<PermissionEntity> findByName(String name);

    /**
     * 根据类型查找权限
     */
    List<PermissionEntity> findByType(PermissionEntity.PermissionType type);

    /**
     * 根据资源类型查找权限
     */
    List<PermissionEntity> findByResourceType(String resourceType);

    /**
     * 根据操作类型查找权限
     */
    List<PermissionEntity> findByAction(PermissionEntity.PermissionAction action);

    /**
     * 根据范围查找权限
     */
    List<PermissionEntity> findByScope(PermissionEntity.PermissionScope scope);

    /**
     * 查找启用的权限
     */
    List<PermissionEntity> findByEnabledTrue();

    /**
     * 查找系统内置权限
     */
    List<PermissionEntity> findBySystemTrue();

    /**
     * 根据资源类型和操作查找权限
     */
    Optional<PermissionEntity> findByResourceTypeAndAction(
            String resourceType, PermissionEntity.PermissionAction action);

    /**
     * 检查权限名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 查找全局权限
     */
    @Query("SELECT p FROM PermissionEntity p WHERE p.scope = 'GLOBAL' AND p.enabled = true")
    List<PermissionEntity> findGlobalPermissions();

    /**
     * 查找游戏级权限
     */
    @Query("SELECT p FROM PermissionEntity p WHERE p.scope = 'GAME' AND p.enabled = true")
    List<PermissionEntity> findGamePermissions();

    /**
     * 查找环境级权限
     */
    @Query("SELECT p FROM PermissionEntity p WHERE p.scope = 'ENVIRONMENT' AND p.enabled = true")
    List<PermissionEntity> findEnvironmentPermissions();

    /**
     * 根据资源类型查找启用的权限
     */
    @Query("SELECT p FROM PermissionEntity p WHERE p.resourceType = :resourceType AND p.enabled = true")
    List<PermissionEntity> findByResourceTypeEnabled(@Param("resourceType") String resourceType);
}