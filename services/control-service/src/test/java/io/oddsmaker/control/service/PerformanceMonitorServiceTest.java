package io.oddsmaker.control.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class PerformanceMonitorServiceTest {

    private PerformanceMonitorService performanceMonitorService;
    private MeterRegistry meterRegistry;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        performanceMonitorService = new PerformanceMonitorService(meterRegistry);
    }

    @Test
    void recordApiRequest_IncrementsCounter() {
        performanceMonitorService.recordApiRequest("GET", "/api/games", 200);

        Counter counter = meterRegistry.find("oddsmaker.api.requests.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void recordApiRequest_Error_IncrementsErrorCounter() {
        performanceMonitorService.recordApiRequest("GET", "/api/games", 500);

        Counter totalCounter = meterRegistry.find("oddsmaker.api.requests.total").counter();
        Counter errorCounter = meterRegistry.find("oddsmaker.api.errors.total").counter();

        assertNotNull(totalCounter);
        assertNotNull(errorCounter);
        assertEquals(1.0, totalCounter.count());
        assertEquals(1.0, errorCounter.count());
    }

    @Test
    void recordApiResponseTime_RecordsTimer() {
        performanceMonitorService.recordApiResponseTime("GET", "/api/games", 150);

        Timer timer = meterRegistry.find("oddsmaker.api.response.time").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) >= 150);
    }

    @Test
    void recordEventIngested_IncrementsCounter() {
        performanceMonitorService.recordEventIngested("game_123", "prod", "business");

        Counter counter = meterRegistry.find("oddsmaker.events.ingested.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void recordEventProcessed_IncrementsCounterAndTimer() {
        performanceMonitorService.recordEventProcessed("game_123", "prod", "business", 50);

        Counter counter = meterRegistry.find("oddsmaker.events.processed.total").counter();
        Timer timer = meterRegistry.find("oddsmaker.events.processing.time").timer();

        assertNotNull(counter);
        assertNotNull(timer);
        assertEquals(1.0, counter.count());
        assertEquals(1, timer.count());
    }

    @Test
    void recordRiskEventDetected_IncrementsCounter() {
        performanceMonitorService.recordRiskEventDetected("game_123", "prod", "fraud", "high");

        Counter counter = meterRegistry.find("oddsmaker.risk.events.detected.total").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void recordRiskActionExecuted_IncrementsCounter() {
        performanceMonitorService.recordRiskActionExecuted("game_123", "prod", "block", "fraud");

        Counter counter = meterRegistry.find("oddsmaker.risk.actions.executed").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void recordDatabaseQuery_RecordsTimer() {
        performanceMonitorService.recordDatabaseQuery("SELECT", "games", 25);

        Timer timer = meterRegistry.find("oddsmaker.database.query.time").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void recordKafkaPublish_RecordsTimer() {
        performanceMonitorService.recordKafkaPublish("oddsmaker.events_raw", 10);

        Timer timer = meterRegistry.find("oddsmaker.kafka.publish.time").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void updateActiveUsers_UpdatesGauge() {
        performanceMonitorService.updateActiveUsers(100);

        var gauge = meterRegistry.find("oddsmaker.users.active").gauge();
        assertNotNull(gauge);
        assertEquals(100.0, gauge.value());
    }

    @Test
    void updateQueueSize_UpdatesGauge() {
        performanceMonitorService.updateQueueSize(500);

        var gauge = meterRegistry.find("oddsmaker.queue.size").gauge();
        assertNotNull(gauge);
        assertEquals(500.0, gauge.value());
    }

    @Test
    void updateMemoryUsage_UpdatesGauge() {
        performanceMonitorService.updateMemoryUsage(1024 * 1024 * 100);

        var gauge = meterRegistry.find("oddsmaker.memory.usage").gauge();
        assertNotNull(gauge);
        assertEquals(1024 * 1024 * 100, gauge.value());
    }

    @Test
    void updateCpuUsage_UpdatesGauge() {
        performanceMonitorService.updateCpuUsage(75);

        var gauge = meterRegistry.find("oddsmaker.cpu.usage").gauge();
        assertNotNull(gauge);
        assertEquals(75.0, gauge.value());
    }

    @Test
    void incrementCustomCounter_IncrementsCounter() {
        performanceMonitorService.incrementCustomCounter("custom.counter", "tag1", "value1");

        Counter counter = meterRegistry.find("custom.counter").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void recordCustomTimer_RecordsTimer() {
        performanceMonitorService.recordCustomTimer("custom.timer", 100, "tag1", "value1");

        Timer timer = meterRegistry.find("custom.timer").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
    }

    @Test
    void recordUserLogin_IncrementsCounter() {
        performanceMonitorService.recordUserLogin("game_123", "email");

        Counter counter = meterRegistry.find("oddsmaker.users.login").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void recordExperimentExposure_IncrementsCounter() {
        performanceMonitorService.recordExperimentExposure("game_123", "exp_1", "A");

        Counter counter = meterRegistry.find("oddsmaker.experiments.exposure").counter();
        assertNotNull(counter);
        assertEquals(1.0, counter.count());
    }

    @Test
    void recordRevenue_IncrementsCounters() {
        performanceMonitorService.recordRevenue("game_123", "USD", 9.99);

        Counter amountCounter = meterRegistry.find("oddsmaker.revenue.amount").counter();
        Counter countCounter = meterRegistry.find("oddsmaker.revenue.count").counter();

        assertNotNull(amountCounter);
        assertNotNull(countCounter);
        assertEquals(9.99, amountCounter.totalAmount(), 0.01);
        assertEquals(1.0, countCounter.count());
    }

    @Test
    void recordHealthCheck_RecordsGauge() {
        performanceMonitorService.recordHealthCheck("database", true);

        var gauge = meterRegistry.find("oddsmaker.health").tag("component", "database").gauge();
        assertNotNull(gauge);
        assertEquals(1.0, gauge.value());
    }

    @Test
    void recordAvailability_RecordsGauge() {
        performanceMonitorService.recordAvailability("gateway", 99.95);

        var gauge = meterRegistry.find("oddsmaker.availability").tag("service", "gateway").gauge();
        assertNotNull(gauge);
        assertEquals(99.95, gauge.value(), 0.01);
    }

    @Test
    void startAndStopTimer_RecordsDuration() throws InterruptedException {
        Timer.Sample sample = performanceMonitorService.startTimer();
        
        Thread.sleep(100); // 模拟一些处理时间
        
        performanceMonitorService.stopTimer(sample, "test.timer", "tag1", "value1");

        Timer timer = meterRegistry.find("test.timer").timer();
        assertNotNull(timer);
        assertEquals(1, timer.count());
        assertTrue(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS) >= 100);
    }

    @Test
    void getMeterRegistry_ReturnsRegistry() {
        MeterRegistry registry = performanceMonitorService.getMeterRegistry();
        assertNotNull(registry);
        assertEquals(meterRegistry, registry);
    }
}