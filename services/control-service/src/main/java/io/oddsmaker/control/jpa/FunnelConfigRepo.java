package io.oddsmaker.control.jpa;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 漏斗配置数据访问接口
 */
@Repository
public interface FunnelConfigRepo extends JpaRepository<FunnelConfigEntity, String> {

    /**
     * 根据游戏ID查找漏斗配置
     */
    Page<FunnelConfigEntity> findByGameIdAndDeletedAtIsNull(String gameId, Pageable pageable);

    /**
     * 根据游戏ID和类型查找漏斗配置
     */
    List<FunnelConfigEntity> findByGameIdAndTypeAndDeletedAtIsNull(String gameId, FunnelConfigEntity.FunnelType type);

    /**
     * 查找启用的漏斗配置
     */
    List<FunnelConfigEntity> findByGameIdAndEnabledTrueAndDeletedAtIsNull(String gameId);

    /**
     * 根据名称搜索漏斗配置
     */
    @Query("SELECT f FROM FunnelConfigEntity f WHERE f.gameId = :gameId AND " +
           "(LOWER(f.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(f.description) LIKE LOWER(CONCAT('%', :query, '%'))) AND " +
           "f.deletedAt IS NULL")
    Page<FunnelConfigEntity> searchByName(@Param("gameId") String gameId, @Param("query") String query, Pageable pageable);

    /**
     * 检查漏斗名称是否存在
     */
    boolean existsByGameIdAndNameAndDeletedAtIsNull(String gameId, String name);

    /**
     * 统计游戏的漏斗数量
     */
    long countByGameIdAndDeletedAtIsNull(String gameId);

    /**
     * 统计启用的漏斗数量
     */
    long countByGameIdAndEnabledTrueAndDeletedAtIsNull(String gameId);
}