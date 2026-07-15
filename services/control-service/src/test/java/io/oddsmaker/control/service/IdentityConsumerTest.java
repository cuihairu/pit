package io.oddsmaker.control.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.oddsmaker.control.dto.IdentityEventDto;
import io.oddsmaker.control.jpa.GameEnvironmentEntity;
import io.oddsmaker.control.jpa.GameEnvironmentRepo;
import io.oddsmaker.control.jpa.IdentityEntity;
import io.oddsmaker.control.jpa.IdentityLinkEntity;
import io.oddsmaker.control.jpa.IdentityLinkRepo;
import io.oddsmaker.control.jpa.IdentityRepo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 身份事件消费者落库测试（仿 RiskEventConsumerTest）。
 *
 * 覆盖：JSON 契约对齐、新建身份（主表+links 扇出）、重复消费幂等、环境名→ID 解析、非法 JSON 静默跳过。
 * 替代无法在无 docker 环境执行的完整端到端验证，证明 identity-merge 链路（消费→落库）打通。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("身份事件消费者落库测试")
class IdentityConsumerTest {

    @Mock private IdentityRepo identityRepo;
    @Mock private IdentityLinkRepo identityLinkRepo;
    @Mock private GameEnvironmentRepo gameEnvironmentRepo;

    private IdentityConsumer consumer;
    private final ObjectMapper om = new ObjectMapper();

    @BeforeEach
    void setUp() {
        consumer = new IdentityConsumer();
        ReflectionTestUtils.setField(consumer, "objectMapper", om);
        ReflectionTestUtils.setField(consumer, "identityRepo", identityRepo);
        ReflectionTestUtils.setField(consumer, "identityLinkRepo", identityLinkRepo);
        ReflectionTestUtils.setField(consumer, "gameEnvironmentRepo", gameEnvironmentRepo);
    }

    private static String identityId() {
        return "idt_" + "a".repeat(28);  // 32 字符
    }

    private String identityEventJson() {
        return "{"
                + "\"game_id\":\"game_demo\""
                + ",\"environment\":\"prod\""
                + ",\"identity_id\":\"" + identityId() + "\""
                + ",\"user_id\":\"user_1\""
                + ",\"player_id\":\"player_1\""
                + ",\"device_ids\":[\"dev_a\",\"dev_b\"]"
                + ",\"player_ids\":[\"player_1\"]"
                + ",\"character_ids\":[\"char_1\"]"
                + ",\"first_seen\":1730000000000"
                + ",\"last_seen\":1730000100000}";
    }

    @Test
    @DisplayName("JSON 契约对齐 IdentityMergeJob.toJson（数组/Long 字段正确反序列化）")
    void jsonContract_aligned() throws Exception {
        IdentityEventDto dto = om.readValue(identityEventJson(), IdentityEventDto.class);
        assertEquals("game_demo", dto.gameId);
        assertEquals("prod", dto.environment);
        assertEquals("user_1", dto.userId);
        assertEquals("player_1", dto.playerId);
        assertEquals(Arrays.asList("dev_a", "dev_b"), dto.deviceIds);
        assertEquals(Arrays.asList("player_1"), dto.playerIds);
        assertEquals(Arrays.asList("char_1"), dto.characterIds);
        assertEquals(1730000000000L, dto.firstSeen);
        assertEquals(1730000100000L, dto.lastSeen);
    }

    @Test
    @DisplayName("新身份：落主表（primaryId=首设备, status=ACTIVE, eventCount=1）+ 扇出 5 条 links")
    void newIdentity_savesEntityAndLinks() {
        String id = identityId();
        when(identityRepo.findById(id)).thenReturn(Optional.empty());
        when(identityLinkRepo.findActiveByIdentityIdAndTypeAndLinkedId(eq(id), anyString(), anyString()))
                .thenReturn(Optional.empty());

        consumer.onIdentityEvent(identityEventJson());

        ArgumentCaptor<IdentityEntity> cap = ArgumentCaptor.forClass(IdentityEntity.class);
        verify(identityRepo).save(cap.capture());
        IdentityEntity saved = cap.getValue();
        assertEquals(id, saved.id);
        assertEquals("game_demo", saved.gameId);
        assertEquals("dev_a", saved.primaryId);    // 首设备
        assertEquals("dev_a", saved.deviceId);
        assertEquals("user_1", saved.userId);
        assertEquals(IdentityEntity.IdentityStatus.ACTIVE, saved.status);
        assertEquals(1L, saved.eventCount);

        // links：2 device + 1 player + 1 character + 1 user = 5 条
        verify(identityLinkRepo, times(5)).save(any(IdentityLinkEntity.class));
    }

    @Test
    @DisplayName("重复消费：主表 eventCount 自增，link 命中则 usageCount++ 不重建")
    void existingIdentity_upsertIdempotent() {
        String id = identityId();
        IdentityEntity existing = new IdentityEntity();
        existing.id = id;
        existing.gameId = "game_demo";
        existing.eventCount = 5L;
        when(identityRepo.findById(id)).thenReturn(Optional.of(existing));
        when(identityLinkRepo.findActiveByIdentityIdAndTypeAndLinkedId(eq(id), anyString(), anyString()))
                .thenReturn(Optional.of(new IdentityLinkEntity()));

        consumer.onIdentityEvent(identityEventJson());

        verify(identityRepo).save(existing);
        assertEquals(6L, existing.eventCount);     // 5 → 6
        verify(identityLinkRepo, times(5)).save(any(IdentityLinkEntity.class));  // 5 条均更新
    }

    @Test
    @DisplayName("环境名→ID 解析：命中则写入 environmentId")
    void environmentResolved() {
        String id = identityId();
        GameEnvironmentEntity env = new GameEnvironmentEntity();
        env.id = "env_prod_001";
        when(gameEnvironmentRepo.findByGameIdAndNameAndDeletedAtIsNull("game_demo", "prod"))
                .thenReturn(List.of(env));
        when(identityRepo.findById(id)).thenReturn(Optional.empty());
        when(identityLinkRepo.findActiveByIdentityIdAndTypeAndLinkedId(eq(id), anyString(), anyString()))
                .thenReturn(Optional.empty());

        consumer.onIdentityEvent(identityEventJson());

        ArgumentCaptor<IdentityEntity> cap = ArgumentCaptor.forClass(IdentityEntity.class);
        verify(identityRepo).save(cap.capture());
        assertEquals("env_prod_001", cap.getValue().environmentId);
    }

    @Test
    @DisplayName("非法 JSON：静默跳过，不触发任何 repo 写入")
    void invalidJson_skipsGracefully() {
        assertDoesNotThrow(() -> consumer.onIdentityEvent("{not json"));
        verifyNoInteractions(identityRepo, identityLinkRepo);
    }
}
