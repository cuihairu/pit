package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 角色数据访问接口
 */
@Repository
public interface RoleRepo extends JpaRepository<RoleEntity, String> {

    /**
     * 根据名称查找角色
     */
    Optional<RoleEntity> findByName(String name);

    /**
     * 根据类型查找角色
     */
    List<RoleEntity> findByType(RoleEntity.RoleType type);

    /**
     * 查找启用的角色
     */
    List<RoleEntity> findByEnabledTrue();

    /**
     * 查找系统内置角色
     */
    List<RoleEntity> findBySystemTrue();

    /**
     * 根据级别查找角色
     */
    List<RoleEntity> findByLevelGreaterThanEqual(Integer level);

    /**
     * 检查角色名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 查找可管理的角色（级别低于指定级别）
     */
    @Query("SELECT r FROM RoleEntity r WHERE r.level < :level AND r.enabled = true ORDER BY r.level DESC")
    List<RoleEntity> findManageableRoles(@Param("level") Integer level);

    /**
     * 查找包含指定权限的角色
     */
    @Query("SELECT r FROM RoleEntity r JOIN r.permissions p WHERE p.id = :permissionId AND r.enabled = true")
    List<RoleEntity> findByPermissionId(@Param("permissionId") String permissionId);

    /**
     * 查找自定义角色
     */
    @Query("SELECT r FROM RoleEntity r WHERE r.type = 'CUSTOM' AND r.enabled = true ORDER BY r.name")
    List<RoleEntity> findCustomRoles();

    /**
     * 获取角色统计信息
     */
    @Query("SELECT new map(" +
           "COUNT(r) as totalRoles, " +
           "COUNT(CASE WHEN r.type = 'SYSTEM' THEN 1 END) as systemRoles, " +
           "COUNT(CASE WHEN r.type = 'CUSTOM' THEN 1 END) as customRoles, " +
           "COUNT(CASE WHEN r.enabled = true THEN 1 END) as enabledRoles" +
           ") FROM RoleEntity r")
    List<Object> getRoleStatistics();
}