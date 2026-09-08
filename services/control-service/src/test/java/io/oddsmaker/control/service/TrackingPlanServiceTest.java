package io.oddsmaker.control.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.oddsmaker.control.dto.EventDefinitionDTO;
import io.oddsmaker.control.dto.EventPropertyDefinitionDTO;
import io.oddsmaker.control.dto.TrackingPlanDTO;
import io.oddsmaker.control.jpa.EventDefinitionEntity;
import io.oddsmaker.control.jpa.EventDefinitionRepo;
import io.oddsmaker.control.jpa.EventPropertyDefinitionEntity;
import io.oddsmaker.control.jpa.EventPropertyDefinitionRepo;
import io.oddsmaker.control.jpa.GameRepo;
import io.oddsmaker.control.jpa.GameEnvironmentRepo;
import io.oddsmaker.control.jpa.TrackingPlanEntity;
import io.oddsmaker.control.jpa.TrackingPlanRepo;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

/**
 * TrackingPlanService 单元测试：字段字典规格（枚举/数组/cardinality 上限）
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TrackingPlanService 单元测试")
class TrackingPlanServiceTest {

    @Mock
    private TrackingPlanRepo trackingPlanRepo;

    @Mock
    private EventDefinitionRepo eventDefinitionRepo;

    @Mock
    private EventPropertyDefinitionRepo propertyDefinitionRepo;

    @Mock
    private GameRepo gameRepo;

    @Mock
    private GameEnvironmentRepo environmentRepo;

    @Mock
    private AuditLogService auditLog;

    @InjectMocks
    private TrackingPlanService service;

    private static TrackingPlanEntity draftPlan(String planId) {
        TrackingPlanEntity plan = new TrackingPlanEntity();
        plan.id = planId;
        plan.gameId = "game_1";
        plan.status = TrackingPlanEntity.PlanStatus.DRAFT;
        return plan;
    }

    private static EventDefinitionEntity eventDef(String id, String planId) {
        EventDefinitionEntity def = new EventDefinitionEntity();
        def.id = id;
        def.trackingPlanId = planId;
        def.eventName = "level_complete";
        def.eventType = "progression";
        return def;
    }

    private void stubDraftContext(String eventId, String planId) {
        EventDefinitionEntity def = eventDef(eventId, planId);
        when(eventDefinitionRepo.findById(eventId)).thenReturn(Optional.of(def));
        when(trackingPlanRepo.findByIdAndDeletedAtIsNull(planId)).thenReturn(Optional.of(draftPlan(planId)));
    }

    @Test
    @DisplayName("枚举属性：合法 allowedValues 创建成功")
    void enumPropertyWithValidValuesCreated() {
        stubDraftContext("evd_1", "tp_1");
        when(propertyDefinitionRepo.findByEventDefinitionIdAndPropertyName("evd_1", "result")).thenReturn(Optional.empty());
        when(propertyDefinitionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventPropertyDefinitionDTO dto = new EventPropertyDefinitionDTO();
        dto.propertyName = "result";
        dto.type = EventPropertyDefinitionEntity.PropertyType.ENUM;
        dto.allowedValues = "[\"win\",\"lose\",\"draw\"]";
        dto.cardinalityLimit = 3;

        EventPropertyDefinitionDTO created = service.createPropertyDefinition("evd_1", dto);

        assertEquals("result", created.propertyName);
        assertEquals(3, created.cardinalityLimit);
        verify(auditLog).logCreate(eq("event_property"), any(), eq("result"),
            eq("api"), eq("api"), isNull(), anyMap());
    }

    @Test
    @DisplayName("枚举属性：缺少 allowedValues 被拒绝")
    void enumPropertyWithoutValuesRejected() {
        stubDraftContext("evd_2", "tp_1");
        when(propertyDefinitionRepo.findByEventDefinitionIdAndPropertyName("evd_2", "state")).thenReturn(Optional.empty());

        EventPropertyDefinitionDTO dto = new EventPropertyDefinitionDTO();
        dto.propertyName = "state";
        dto.type = EventPropertyDefinitionEntity.PropertyType.ENUM;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.createPropertyDefinition("evd_2", dto));
        assertTrue(ex.getMessage().contains("allowedValues"));
        verify(propertyDefinitionRepo, never()).save(any());
    }

    @Test
    @DisplayName("枚举属性：重复枚举值被拒绝")
    void enumPropertyWithDuplicateValuesRejected() {
        stubDraftContext("evd_3", "tp_1");
        when(propertyDefinitionRepo.findByEventDefinitionIdAndPropertyName("evd_3", "grade")).thenReturn(Optional.empty());

        EventPropertyDefinitionDTO dto = new EventPropertyDefinitionDTO();
        dto.propertyName = "grade";
        dto.type = EventPropertyDefinitionEntity.PropertyType.ENUM;
        dto.allowedValues = "[\"a\",\"b\",\"a\"]";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.createPropertyDefinition("evd_3", dto));
        assertTrue(ex.getMessage().contains("duplicates"));
    }

    @Test
    @DisplayName("cardinality 上限：小于枚举候选数被拒绝")
    void cardinalityBelowEnumSizeRejected() {
        stubDraftContext("evd_4", "tp_1");
        when(propertyDefinitionRepo.findByEventDefinitionIdAndPropertyName("evd_4", "tier")).thenReturn(Optional.empty());

        EventPropertyDefinitionDTO dto = new EventPropertyDefinitionDTO();
        dto.propertyName = "tier";
        dto.type = EventPropertyDefinitionEntity.PropertyType.ENUM;
        dto.allowedValues = "[\"gold\",\"silver\",\"bronze\"]";
        dto.cardinalityLimit = 2;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.createPropertyDefinition("evd_4", dto));
        assertTrue(ex.getMessage().contains("cardinalityLimit"));
    }

    @Test
    @DisplayName("cardinality 上限：非正值被拒绝")
    void nonPositiveCardinalityRejected() {
        stubDraftContext("evd_5", "tp_1");
        when(propertyDefinitionRepo.findByEventDefinitionIdAndPropertyName("evd_5", "level_name")).thenReturn(Optional.empty());

        EventPropertyDefinitionDTO dto = new EventPropertyDefinitionDTO();
        dto.propertyName = "level_name";
        dto.type = EventPropertyDefinitionEntity.PropertyType.STRING;
        dto.cardinalityLimit = 0;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.createPropertyDefinition("evd_5", dto));
        assertTrue(ex.getMessage().contains("cardinalityLimit"));
    }

    @Test
    @DisplayName("普通字符串属性可设置独立 cardinality 上限")
    void stringPropertyWithCardinalityLimitAccepted() {
        stubDraftContext("evd_6", "tp_1");
        when(propertyDefinitionRepo.findByEventDefinitionIdAndPropertyName("evd_6", "level_name")).thenReturn(Optional.empty());
        when(propertyDefinitionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventPropertyDefinitionDTO dto = new EventPropertyDefinitionDTO();
        dto.propertyName = "level_name";
        dto.type = EventPropertyDefinitionEntity.PropertyType.STRING;
        dto.maxLength = 64;
        dto.cardinalityLimit = 5000;

        EventPropertyDefinitionDTO created = service.createPropertyDefinition("evd_6", dto);
        assertEquals(5000, created.cardinalityLimit);
    }

    @Test
    @DisplayName("数组属性：缺少元素类型被拒绝")
    void arrayPropertyWithoutElementTypeRejected() {
        stubDraftContext("evd_7", "tp_1");
        when(propertyDefinitionRepo.findByEventDefinitionIdAndPropertyName("evd_7", "tags")).thenReturn(Optional.empty());

        EventPropertyDefinitionDTO dto = new EventPropertyDefinitionDTO();
        dto.propertyName = "tags";
        dto.type = EventPropertyDefinitionEntity.PropertyType.ARRAY;

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.createPropertyDefinition("evd_7", dto));
        assertTrue(ex.getMessage().contains("arrayElementType"));
    }

    @Test
    @DisplayName("属性更新：draft 计划内可收紧 cardinality 上限")
    void propertyUpdateChangesCardinality() {
        stubDraftContext("evd_8", "tp_1");
        EventPropertyDefinitionEntity existing = new EventPropertyDefinitionEntity();
        existing.id = "epd_8";
        existing.eventDefinitionId = "evd_8";
        existing.propertyName = "level_name";
        existing.type = EventPropertyDefinitionEntity.PropertyType.STRING;
        existing.cardinalityLimit = 5000;
        when(propertyDefinitionRepo.findById("epd_8")).thenReturn(Optional.of(existing));
        when(propertyDefinitionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventPropertyDefinitionDTO dto = new EventPropertyDefinitionDTO();
        dto.cardinalityLimit = 1000;

        EventPropertyDefinitionDTO updated = service.updatePropertyDefinition("evd_8", "epd_8", dto);
        assertEquals(1000, updated.cardinalityLimit);
        verify(auditLog).logUpdate(eq("event_property"), eq("epd_8"), eq("level_name"),
            eq("api"), eq("api"), isNull(), anyMap());
    }

    @Test
    @DisplayName("属性更新：ENUM 补充校验同样生效")
    void propertyUpdateToEnumValidated() {
        stubDraftContext("evd_9", "tp_1");
        EventPropertyDefinitionEntity existing = new EventPropertyDefinitionEntity();
        existing.id = "epd_9";
        existing.eventDefinitionId = "evd_9";
        existing.propertyName = "result";
        existing.type = EventPropertyDefinitionEntity.PropertyType.STRING;
        when(propertyDefinitionRepo.findById("epd_9")).thenReturn(Optional.of(existing));
        when(propertyDefinitionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        EventPropertyDefinitionDTO dto = new EventPropertyDefinitionDTO();
        dto.type = EventPropertyDefinitionEntity.PropertyType.ENUM;
        dto.allowedValues = "[\"ok\"]";
        dto.cardinalityLimit = 5; // > 1，合法

        EventPropertyDefinitionDTO updated = service.updatePropertyDefinition("evd_9", "epd_9", dto);
        assertEquals(EventPropertyDefinitionEntity.PropertyType.ENUM, updated.type);
    }

    @Test
    @DisplayName("属性删除：软删除并记录审计")
    void propertyDeleteSoftDeletes() {
        stubDraftContext("evd_10", "tp_1");
        EventPropertyDefinitionEntity existing = new EventPropertyDefinitionEntity();
        existing.id = "epd_10";
        existing.eventDefinitionId = "evd_10";
        existing.propertyName = "legacy";
        when(propertyDefinitionRepo.findById("epd_10")).thenReturn(Optional.of(existing));
        when(propertyDefinitionRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.deletePropertyDefinition("evd_10", "epd_10");

        assertNotNull(existing.deletedAt);
        verify(auditLog).logDelete("event_property", "epd_10", "legacy", "api", "api", null);
    }
}
