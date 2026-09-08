package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.AnnouncementEntity;
import io.oddsmaker.control.security.AccessGuard;
import io.oddsmaker.control.service.AnnouncementService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 公告系统 API。
 * 运营端：/api/games/{gameId}/announcements（CRUD + publish/schedule/offline）；
 * 游戏服拉取：/api/announcements/active。
 */
@RestController
public class AnnouncementController {

    private final AnnouncementService announcementService;
    private final AccessGuard accessGuard;

    public AnnouncementController(AnnouncementService announcementService, AccessGuard accessGuard) {
        this.announcementService = announcementService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/api/games/{gameId}/announcements")
    public ResponseEntity<List<AnnouncementEntity>> list(@PathVariable String gameId) {
        accessGuard.requireGamePermission(gameId, "game:read");
        return ResponseEntity.ok(announcementService.list(gameId));
    }

    @GetMapping("/api/games/{gameId}/announcements/{id}")
    public ResponseEntity<AnnouncementEntity> get(@PathVariable String gameId, @PathVariable String id) {
        accessGuard.requireGamePermission(gameId, "game:read");
        AnnouncementEntity announcement = announcementService.get(id);
        if (announcement == null || !announcement.gameId.equals(gameId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(announcement);
    }

    @PostMapping("/api/games/{gameId}/announcements")
    public ResponseEntity<AnnouncementEntity> create(@PathVariable String gameId,
                                                     @RequestBody AnnouncementEntity announcement) {
        accessGuard.requireGamePermission(gameId, "game:update");
        announcement.gameId = gameId;
        return ResponseEntity.ok(announcementService.create(announcement, currentOperator()));
    }

    @PutMapping("/api/games/{gameId}/announcements/{id}")
    public ResponseEntity<AnnouncementEntity> update(@PathVariable String gameId,
                                                     @PathVariable String id,
                                                     @RequestBody AnnouncementEntity req) {
        accessGuard.requireGamePermission(gameId, "game:update");
        AnnouncementEntity existing = announcementService.get(id);
        if (existing == null || !existing.gameId.equals(gameId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(announcementService.update(id, req, currentOperator()));
    }

    @PostMapping("/api/games/{gameId}/announcements/{id}/publish")
    public ResponseEntity<AnnouncementEntity> publish(@PathVariable String gameId, @PathVariable String id) {
        accessGuard.requireGamePermission(gameId, "game:update");
        AnnouncementEntity existing = announcementService.get(id);
        if (existing == null || !existing.gameId.equals(gameId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(announcementService.publish(id, currentOperator()));
    }

    @PostMapping("/api/games/{gameId}/announcements/{id}/schedule")
    public ResponseEntity<AnnouncementEntity> schedule(@PathVariable String gameId,
                                                       @PathVariable String id,
                                                       @RequestBody ScheduleReq req) {
        accessGuard.requireGamePermission(gameId, "game:update");
        AnnouncementEntity existing = announcementService.get(id);
        if (existing == null || !existing.gameId.equals(gameId)) {
            return ResponseEntity.notFound().build();
        }
        LocalDateTime at = req.scheduledAt != null ? LocalDateTime.parse(req.scheduledAt) : null;
        return ResponseEntity.ok(announcementService.schedule(id, at, currentOperator()));
    }

    @PostMapping("/api/games/{gameId}/announcements/{id}/offline")
    public ResponseEntity<AnnouncementEntity> offline(@PathVariable String gameId, @PathVariable String id) {
        accessGuard.requireGamePermission(gameId, "game:update");
        AnnouncementEntity existing = announcementService.get(id);
        if (existing == null || !existing.gameId.equals(gameId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(announcementService.offline(id, currentOperator()));
    }

    @DeleteMapping("/api/games/{gameId}/announcements/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String gameId, @PathVariable String id) {
        accessGuard.requireGamePermission(gameId, "game:update");
        AnnouncementEntity existing = announcementService.get(id);
        if (existing == null || !existing.gameId.equals(gameId)) {
            return ResponseEntity.notFound().build();
        }
        boolean deleted = announcementService.delete(id, currentOperator());
        return deleted
            ? ResponseEntity.ok(Map.of("deleted", true, "id", id))
            : ResponseEntity.notFound().build();
    }

    /** 游戏服拉取当前活跃公告（展示窗口内、环境匹配或全环境） */
    @GetMapping("/api/announcements/active")
    public ResponseEntity<List<AnnouncementEntity>> listActive(
            @RequestParam String gameId,
            @RequestParam(value = "environmentId", required = false) String environmentId) {
        accessGuard.requireGamePermission(gameId, "game:read");
        return ResponseEntity.ok(announcementService.listActive(gameId, environmentId));
    }

    public static class ScheduleReq {
        public String scheduledAt;  // ISO-8601 本地时间
    }

    private String currentOperator() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        return auth != null ? auth.getName() : "api";
    }
}
