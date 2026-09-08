package io.oddsmaker.control.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.oddsmaker.control.dto.GameDTO;
import io.oddsmaker.control.jpa.GameEntity;
import io.oddsmaker.control.jpa.GameRepo;
import io.oddsmaker.control.jpa.GameEnvironmentRepo;
import io.oddsmaker.control.jpa.ApiKeyRepo;
import io.oddsmaker.control.jpa.StorageProfileRepo;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

/**
 * GameService 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GameService 单元测试")
class GameServiceTest {

    @Mock
    private GameRepo gameRepo;

    @Mock
    private GameEnvironmentRepo gameEnvironmentRepo;

    @Mock
    private ApiKeyRepo apiKeyRepo;

    @Mock
    private StorageProfileRepo storageProfileRepo;

    @Mock
    private AuditLogService auditLog;

    @InjectMocks
    private GameService gameService;

    private GameEntity savedGame(GameEntity entity) {
        // 模拟 JPA save：回填并返回同一实例
        return entity;
    }

    @Test
    @DisplayName("创建游戏：默认时区/货币生效并记录审计")
    void createGameAppliesDefaultsAndAudits() {
        when(gameRepo.existsById(any())).thenReturn(false);
        when(gameRepo.save(any())).thenAnswer(inv -> savedGame(inv.getArgument(0)));
        when(storageProfileRepo.existsById(any())).thenReturn(true);
        when(gameEnvironmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GameDTO dto = new GameDTO();
        dto.name = "Demo RPG";
        dto.platforms = Set.of(GameEntity.GamePlatform.MOBILE);

        GameDTO created = gameService.createGame(dto);

        assertNotNull(created.id);
        assertEquals("UTC", created.defaultTimezone);
        assertEquals("USD", created.defaultCurrency);
        assertEquals(GameEntity.GameStatus.DEVELOPMENT, created.status);
        verify(auditLog).logCreate(eq("game"), eq(created.id), eq("Demo RPG"),
            eq("api"), eq("api"), isNull(), anyMap());
    }

    @Test
    @DisplayName("创建游戏：自定义时区/货币透传")
    void createGameKeepsExplicitTimezoneAndCurrency() {
        when(gameRepo.existsById(any())).thenReturn(false);
        when(gameRepo.save(any())).thenAnswer(inv -> savedGame(inv.getArgument(0)));
        when(storageProfileRepo.existsById(any())).thenReturn(true);
        when(gameEnvironmentRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        GameDTO dto = new GameDTO();
        dto.name = "CN Game";
        dto.defaultTimezone = "Asia/Shanghai";
        dto.defaultCurrency = "CNY";

        GameDTO created = gameService.createGame(dto);

        assertEquals("Asia/Shanghai", created.defaultTimezone);
        assertEquals("CNY", created.defaultCurrency);
    }

    @Test
    @DisplayName("创建游戏：非法时区被拒绝")
    void createGameRejectsInvalidTimezone() {
        GameDTO dto = new GameDTO();
        dto.name = "Bad TZ";
        dto.defaultTimezone = "Mars/Olympus_Mons";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> gameService.createGame(dto));
        assertTrue(ex.getMessage().contains("Unknown timezone"));
    }

    @Test
    @DisplayName("更新游戏：非法时区被拒绝且不落库")
    void updateGameRejectsInvalidTimezone() {
        GameEntity existing = new GameEntity();
        existing.id = "game_1";
        existing.name = "Existing";
        when(gameRepo.findById("game_1")).thenReturn(Optional.of(existing));

        GameDTO dto = new GameDTO();
        dto.defaultTimezone = "Not/A Zone!";

        assertThrows(IllegalArgumentException.class,
            () -> gameService.updateGame("game_1", dto));
        verify(gameRepo, never()).save(any());
    }

    @Test
    @DisplayName("状态机：DEVELOPMENT 只能转 TESTING")
    void invalidStatusTransitionRejected() {
        GameEntity existing = new GameEntity();
        existing.id = "game_2";
        existing.name = "Dev Game";
        existing.status = GameEntity.GameStatus.DEVELOPMENT;
        when(gameRepo.findById("game_2")).thenReturn(Optional.of(existing));

        GameDTO dto = new GameDTO();
        dto.status = GameEntity.GameStatus.LIVE;

        assertThrows(IllegalArgumentException.class,
            () -> gameService.updateGame("game_2", dto));
    }

    @Test
    @DisplayName("状态机：TESTING 可转 LIVE 并记录审计")
    void validTransitionToLiveAudited() {
        GameEntity existing = new GameEntity();
        existing.id = "game_3";
        existing.name = "Ready Game";
        existing.status = GameEntity.GameStatus.TESTING;
        existing.platforms = Set.of(GameEntity.GamePlatform.WEB);
        when(gameRepo.findById("game_3")).thenReturn(Optional.of(existing));
        when(gameRepo.save(any())).thenAnswer(inv -> savedGame(inv.getArgument(0)));

        GameDTO dto = new GameDTO();
        dto.status = GameEntity.GameStatus.LIVE;

        GameDTO updated = gameService.updateGame("game_3", dto);

        assertEquals(GameEntity.GameStatus.LIVE, updated.status);
        verify(auditLog).logUpdate(eq("game"), eq("game_3"), eq("Ready Game"),
            eq("api"), eq("api"), isNull(), anyMap());
    }

    @Test
    @DisplayName("删除游戏：LIVE 状态不可删除")
    void liveGameCannotBeDeleted() {
        GameEntity existing = new GameEntity();
        existing.id = "game_live";
        existing.name = "Live Game";
        existing.status = GameEntity.GameStatus.LIVE;
        when(gameRepo.findById("game_live")).thenReturn(Optional.of(existing));

        assertThrows(IllegalStateException.class, () -> gameService.deleteGame("game_live"));
    }

    @Test
    @DisplayName("删除游戏：软删除并级联处置环境与密钥")
    void deleteGameSoftDeletesAndCascades() {
        GameEntity existing = new GameEntity();
        existing.id = "game_del";
        existing.name = "Old Game";
        existing.status = GameEntity.GameStatus.MAINTENANCE;
        when(gameRepo.findById("game_del")).thenReturn(Optional.of(existing));
        when(gameRepo.save(any())).thenAnswer(inv -> savedGame(inv.getArgument(0)));
        when(gameEnvironmentRepo.findByGameIdAndDeletedAtIsNull("game_del")).thenReturn(List.of());
        when(apiKeyRepo.findByGameIdAndStatus("game_del", io.oddsmaker.control.jpa.ApiKeyEntity.ApiKeyStatus.ACTIVE))
            .thenReturn(List.of());

        gameService.deleteGame("game_del");

        assertNotNull(existing.deletedAt);
        assertEquals(GameEntity.GameStatus.DISCONTINUED, existing.status);
        verify(auditLog).logDelete("game", "game_del", "Old Game", "api", "api", null);
    }
}
