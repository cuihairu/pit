package io.oddsmaker.control.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 漏斗步骤数据访问接口
 */
@Repository
public interface FunnelStepRepo extends JpaRepository<FunnelStepEntity, Long> {

    /**
     * 根据漏斗ID查找步骤
     */
    List<FunnelStepEntity> findByFunnelIdOrderByStepOrderAsc(Long funnelId);

    /**
     * 根据漏斗ID和步骤顺序查找步骤
     */
    FunnelStepEntity findByFunnelIdAndStepOrder(Long funnelId, Integer stepOrder);

    /**
     * 根据事件名称查找步骤
     */
    List<FunnelStepEntity> findByEventName(String eventName);

    /**
     * 根据漏斗ID删除所有步骤
     */
    void deleteByFunnelId(Long funnelId);

    /**
     * 统计漏斗的步骤数量
     */
    @Query("SELECT COUNT(s) FROM FunnelStepEntity s WHERE s.funnel.id = :funnelId")
    long countByFunnelId(@Param("funnelId") Long funnelId);

    /**
     * 查找漏斗的第一个步骤
     */
    @Query("SELECT s FROM FunnelStepEntity s WHERE s.funnel.id = :funnelId ORDER BY s.stepOrder ASC LIMIT 1")
    FunnelStepEntity findFirstStep(@Param("funnelId") Long funnelId);

    /**
     * 查找漏斗的最后一个步骤
     */
    @Query("SELECT s FROM FunnelStepEntity s WHERE s.funnel.id = :funnelId ORDER BY s.stepOrder DESC LIMIT 1")
    FunnelStepEntity findLastStep(@Param("funnelId") Long funnelId);
}