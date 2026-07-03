package io.oddsmaker.control.service;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 性能监控服务
 * 提供系统性能指标收集和监控功能
 */
@Service
public class PerformanceMonitorService {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitorService.class);

    private final MeterRegistry meterRegistry;
    
    // 计数器
    private final Counter apiRequestsCounter;
    private final Counter apiErrorsCounter;
    private final Counter eventsIngestedCounter;
    private final Counter eventsProcessedCounter;
    private final Counter riskEventsDetectedCounter;
    
    // 计时器
    private final Timer apiResponseTimer;
    private final Timer eventProcessingTimer;
    private final Timer databaseQueryTimer;
    private final Timer kafkaPublishTimer;
    
    // 仪表
    private final AtomicLong activeUsers = new AtomicLong(0);
    private final AtomicLong queueSize = new AtomicLong(0);
    private final AtomicLong memoryUsage = new AtomicLong(0);
    private final AtomicLong cpuUsage = new AtomicLong(0);
    
    // 自定义指标
    private final ConcurrentHashMap<String, AtomicLong> customGauges = new ConcurrentHashMap<>();

    @Autowired
    public PerformanceMonitorService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // 初始化计数器
        this.apiRequestsCounter = Counter.builder("oddsmaker.api.requests.total")
            .description("Total number of API requests")
            .register(meterRegistry);
        
        this.apiErrorsCounter = Counter.builder("oddsmaker.api.errors.total")
            .description("Total number of API errors")
            .register(meterRegistry);
        
        this.eventsIngestedCounter = Counter.builder("oddsmaker.events.ingested.total")
            .description("Total number of events ingested")
            .register(meterRegistry);
        
        this.eventsProcessedCounter = Counter.builder("oddsmaker.events.processed.total")
            .description("Total number of events processed")
            .register(meterRegistry);
        
        this.riskEventsDetectedCounter = Counter.builder("oddsmaker.risk.events.detected.total")
            .description("Total number of risk events detected")
            .register(meterRegistry);
        
        // 初始化计时器
        this.apiResponseTimer = Timer.builder("oddsmaker.api.response.time")
            .description("API response time")
            .register(meterRegistry);
        
        this.eventProcessingTimer = Timer.builder("oddsmaker.events.processing.time")
            .description("Event processing time")
            .register(meterRegistry);
        
        this.databaseQueryTimer = Timer.builder("oddsmaker.database.query.time")
            .description("Database query time")
            .register(meterRegistry);
        
        this.kafkaPublishTimer = Timer.builder("oddsmaker.kafka.publish.time")
            .description("Kafka publish time")
            .register(meterRegistry);
        
        // 注册仪表
        Gauge.builder("oddsmaker.users.active", activeUsers, AtomicLong::get)
            .description("Number of active users")
            .register(meterRegistry);
        
        Gauge.builder("oddsmaker.queue.size", queueSize, AtomicLong::get)
            .description("Queue size")
            .register(meterRegistry);
        
        Gauge.builder("oddsmaker.memory.usage", memoryUsage, AtomicLong::get)
            .description("Memory usage in bytes")
            .register(meterRegistry);
        
        Gauge.builder("oddsmaker.cpu.usage", cpuUsage, AtomicLong::get)
            .description("CPU usage percentage")
            .register(meterRegistry);
        
        logger.info("Performance monitor service initialized");
    }

    // API请求监控
    
    /**
     * 记录API请求
     */
    public void recordApiRequest(String method, String endpoint, int statusCode) {
        apiRequestsCounter.increment();
        
        Tags tags = Tags.of(
            Tag.of("method", method),
            Tag.of("endpoint", endpoint),
            Tag.of("status", String.valueOf(statusCode))
        );
        
        meterRegistry.counter("oddsmaker.api.requests", tags).increment();
        
        if (statusCode >= 400) {
            apiErrorsCounter.increment();
            meterRegistry.counter("oddsmaker.api.errors", tags).increment();
        }
    }
    
    /**
     * 记录API响应时间
     */
    public void recordApiResponseTime(String method, String endpoint, long durationMs) {
        apiResponseTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        Tags tags = Tags.of(
            Tag.of("method", method),
            Tag.of("endpoint", endpoint)
        );
        
        meterRegistry.timer("oddsmaker.api.response", tags).record(durationMs, TimeUnit.MILLISECONDS);
    }

    // 事件处理监控
    
    /**
     * 记录事件摄入
     */
    public void recordEventIngested(String gameId, String environment, String eventType) {
        eventsIngestedCounter.increment();
        
        Tags tags = Tags.of(
            Tag.of("game_id", gameId),
            Tag.of("environment", environment),
            Tag.of("event_type", eventType)
        );
        
        meterRegistry.counter("oddsmaker.events.ingested", tags).increment();
    }
    
    /**
     * 记录事件处理
     */
    public void recordEventProcessed(String gameId, String environment, String eventType, long durationMs) {
        eventsProcessedCounter.increment();
        eventProcessingTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        Tags tags = Tags.of(
            Tag.of("game_id", gameId),
            Tag.of("environment", environment),
            Tag.of("event_type", eventType)
        );
        
        meterRegistry.counter("oddsmaker.events.processed", tags).increment();
        meterRegistry.timer("oddsmaker.events.processing", tags).record(durationMs, TimeUnit.MILLISECONDS);
    }

    // 风控监控
    
    /**
     * 记录风控事件检测
     */
    public void recordRiskEventDetected(String gameId, String environment, String riskType, String severity) {
        riskEventsDetectedCounter.increment();
        
        Tags tags = Tags.of(
            Tag.of("game_id", gameId),
            Tag.of("environment", environment),
            Tag.of("risk_type", riskType),
            Tag.of("severity", severity)
        );
        
        meterRegistry.counter("oddsmaker.risk.events.detected", tags).increment();
    }
    
    /**
     * 记录风控动作执行
     */
    public void recordRiskActionExecuted(String gameId, String environment, String action, String riskType) {
        Tags tags = Tags.of(
            Tag.of("game_id", gameId),
            Tag.of("environment", environment),
            Tag.of("action", action),
            Tag.of("risk_type", riskType)
        );
        
        meterRegistry.counter("oddsmaker.risk.actions.executed", tags).increment();
    }

    // 数据库监控
    
    /**
     * 记录数据库查询时间
     */
    public void recordDatabaseQuery(String queryType, String table, long durationMs) {
        databaseQueryTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        Tags tags = Tags.of(
            Tag.of("query_type", queryType),
            Tag.of("table", table)
        );
        
        meterRegistry.timer("oddsmaker.database.query", tags).record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录数据库连接池使用情况
     */
    public void recordDatabaseConnectionPool(String poolName, int active, int idle, int total) {
        Tags tags = Tags.of(Tag.of("pool", poolName));
        
        meterRegistry.gauge("oddsmaker.database.pool.active", tags, active);
        meterRegistry.gauge("oddsmaker.database.pool.idle", tags, idle);
        meterRegistry.gauge("oddsmaker.database.pool.total", tags, total);
    }

    // Kafka监控
    
    /**
     * 记录Kafka发布时间
     */
    public void recordKafkaPublish(String topic, long durationMs) {
        kafkaPublishTimer.record(durationMs, TimeUnit.MILLISECONDS);
        
        Tags tags = Tags.of(Tag.of("topic", topic));
        meterRegistry.timer("oddsmaker.kafka.publish", tags).record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 记录Kafka消费者延迟
     */
    public void recordKafkaConsumerLag(String topic, String group, long lag) {
        Tags tags = Tags.of(
            Tag.of("topic", topic),
            Tag.of("group", group)
        );
        
        meterRegistry.gauge("oddsmaker.kafka.consumer.lag", tags, lag);
    }

    // 系统资源监控
    
    /**
     * 更新活跃用户数
     */
    public void updateActiveUsers(long count) {
        activeUsers.set(count);
    }
    
    /**
     * 更新队列大小
     */
    public void updateQueueSize(long size) {
        queueSize.set(size);
    }
    
    /**
     * 更新内存使用情况
     */
    public void updateMemoryUsage(long bytes) {
        memoryUsage.set(bytes);
    }
    
    /**
     * 更新CPU使用率
     */
    public void updateCpuUsage(long percentage) {
        cpuUsage.set(percentage);
    }

    // 自定义指标
    
    /**
     * 记录自定义计数器
     */
    public void incrementCustomCounter(String name, String... tags) {
        meterRegistry.counter(name, tags).increment();
    }
    
    /**
     * 记录自定义计时器
     */
    public void recordCustomTimer(String name, long durationMs, String... tags) {
        meterRegistry.timer(name, tags).record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    /**
     * 设置自定义仪表
     */
    public void setCustomGauge(String name, long value, String... tags) {
        customGauges.computeIfAbsent(name, k -> new AtomicLong(0)).set(value);
        meterRegistry.gauge(name, Tags.of(tags), customGauges.get(name), AtomicLong::get);
    }

    // 业务指标
    
    /**
     * 记录用户登录
     */
    public void recordUserLogin(String gameId, String method) {
        Tags tags = Tags.of(
            Tag.of("game_id", gameId),
            Tag.of("method", method)
        );
        
        meterRegistry.counter("oddsmaker.users.login", tags).increment();
    }
    
    /**
     * 记录实验曝光
     */
    public void recordExperimentExposure(String gameId, String experimentId, String variant) {
        Tags tags = Tags.of(
            Tag.of("game_id", gameId),
            Tag.of("experiment_id", experimentId),
            Tag.of("variant", variant)
        );
        
        meterRegistry.counter("oddsmaker.experiments.exposure", tags).increment();
    }
    
    /**
     * 记录收入事件
     */
    public void recordRevenue(String gameId, String currency, double amount) {
        Tags tags = Tags.of(
            Tag.of("game_id", gameId),
            Tag.of("currency", currency)
        );
        
        meterRegistry.counter("oddsmaker.revenue.amount", tags).increment(amount);
        meterRegistry.counter("oddsmaker.revenue.count", tags).increment();
    }
    
    /**
     * 记录漏斗转化
     */
    public void recordFunnelConversion(String gameId, String funnelId, int step, double conversionRate) {
        Tags tags = Tags.of(
            Tag.of("game_id", gameId),
            Tag.of("funnel_id", funnelId),
            Tag.of("step", String.valueOf(step))
        );
        
        meterRegistry.gauge("oddsmaker.funnel.conversion", tags, conversionRate);
    }

    // 健康检查指标
    
    /**
     * 记录健康检查状态
     */
    public void recordHealthCheck(String component, boolean healthy) {
        Tags tags = Tags.of(Tag.of("component", component));
        meterRegistry.gauge("oddsmaker.health", tags, healthy ? 1 : 0);
    }
    
    /**
     * 记录服务可用性
     */
    public void recordAvailability(String service, double availability) {
        Tags tags = Tags.of(Tag.of("service", service));
        meterRegistry.gauge("oddsmaker.availability", tags, availability);
    }

    // 辅助方法
    
    /**
     * 创建计时器样本
     */
    public Timer.Sample startTimer() {
        return Timer.start(meterRegistry);
    }
    
    /**
     * 停止计时器并记录
     */
    public void stopTimer(Timer.Sample sample, String name, String... tags) {
        sample.stop(meterRegistry.timer(name, tags));
    }
    
    /**
     * 获取MeterRegistry
     */
    public MeterRegistry getMeterRegistry() {
        return meterRegistry;
    }
}