package io.oddsmaker.control.experiment;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 实验指标快照：聚合管道（Flink/ClickHouse）按窗口回填的每变体聚合值。
 * 控制面基于快照做统计检验与结果展示，不直连事件明细。
 */
@Entity
@Table(name = "experiment_metric_snapshots",
    indexes = {
        @Index(name = "idx_ems_experiment", columnList = "experiment_id"),
        @Index(name = "idx_ems_window", columnList = "experiment_id, window_start")
    })
public class ExperimentMetricSnapshotEntity {

    @Id
    @Column(length = 64)
    public String id;

    @Column(name = "experiment_id", nullable = false, length = 64)
    public String experimentId;

    /** 指标名，对应实验配置 config.metrics 的键 */
    @Column(name = "metric_name", nullable = false, length = 100)
    public String metricName;

    /** 变体名（control / treatment-1 / ...） */
    @Column(nullable = false, length = 100)
    public String variant;

    /** 窗口起点（epoch millis），同一窗口同 (metric, variant) 覆盖更新 */
    @Column(name = "window_start", nullable = false)
    public long windowStart;

    /** 样本数（曝光用户数或事件数，取决于指标口径） */
    @Column(nullable = false)
    public long count;

    /** 数值求和（count 类指标等于 count） */
    @Column(nullable = false)
    public double sum;

    /** 平方和（用于方差/标准差计算） */
    @Column(name = "sum_squares", nullable = false)
    public double sumSquares;

    /** 成功数（转化类指标：达成目标事件的样本数） */
    @Column(name = "successes", nullable = false)
    public long successes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
