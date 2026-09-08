package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.MailClaimEntity;
import io.oddsmaker.control.jpa.MailEntity;
import io.oddsmaker.control.security.AccessGuard;
import io.oddsmaker.control.service.MailService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 运营邮件 API。
 * 运营端：/api/games/{gameId}/mails（创建/发送/列表/删除）；
 * 游戏服：/api/mails/inbox（收件箱）、/api/mails/{id}/claim（领取附件凭据）。
 */
@RestController
public class MailController {

    private final MailService mailService;
    private final AccessGuard accessGuard;

    public MailController(MailService mailService, AccessGuard accessGuard) {
        this.mailService = mailService;
        this.accessGuard = accessGuard;
    }

    @GetMapping("/api/games/{gameId}/mails")
    public ResponseEntity<List<MailEntity>> list(@PathVariable String gameId) {
        accessGuard.requireGamePermission(gameId, "game:read");
        return ResponseEntity.ok(mailService.list(gameId));
    }

    @GetMapping("/api/games/{gameId}/mails/{id}")
    public ResponseEntity<MailEntity> get(@PathVariable String gameId, @PathVariable String id) {
        accessGuard.requireGamePermission(gameId, "game:read");
        MailEntity mail = mailService.get(id);
        if (mail == null || !mail.gameId.equals(gameId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mail);
    }

    @PostMapping("/api/games/{gameId}/mails")
    public ResponseEntity<MailEntity> create(@PathVariable String gameId,
                                             @RequestBody MailEntity mail) {
        accessGuard.requireGamePermission(gameId, "game:update");
        mail.gameId = gameId;
        return ResponseEntity.ok(mailService.create(mail, currentOperator()));
    }

    @PostMapping("/api/games/{gameId}/mails/{id}/send")
    public ResponseEntity<MailEntity> send(@PathVariable String gameId, @PathVariable String id) {
        accessGuard.requireGamePermission(gameId, "game:update");
        MailEntity mail = mailService.get(id);
        if (mail == null || !mail.gameId.equals(gameId)) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(mailService.send(id, currentOperator()));
    }

    @DeleteMapping("/api/games/{gameId}/mails/{id}")
    public ResponseEntity<Map<String, Object>> delete(@PathVariable String gameId, @PathVariable String id) {
        accessGuard.requireGamePermission(gameId, "game:update");
        MailEntity mail = mailService.get(id);
        if (mail == null || !mail.gameId.equals(gameId)) {
            return ResponseEntity.notFound().build();
        }
        boolean deleted = mailService.delete(id, currentOperator());
        return deleted
            ? ResponseEntity.ok(Map.of("deleted", true, "id", id))
            : ResponseEntity.notFound().build();
    }

    /** 游戏服拉取玩家收件箱 */
    @GetMapping("/api/mails/inbox")
    public ResponseEntity<List<MailEntity>> inbox(@RequestParam String gameId,
                                                  @RequestParam String playerKey,
                                                  @RequestParam(value = "environmentId", required = false) String environmentId) {
        accessGuard.requireGamePermission(gameId, "game:read");
        return ResponseEntity.ok(mailService.inbox(gameId, environmentId, playerKey));
    }

    /** 游戏服代玩家领取附件，返回发放凭据（幂等） */
    @PostMapping("/api/mails/{id}/claim")
    public ResponseEntity<Map<String, Object>> claim(@PathVariable String id,
                                                     @RequestParam String gameId,
                                                     @RequestParam String playerKey) {
        accessGuard.requireGamePermission(gameId, "game:read");
        MailClaimEntity claim = mailService.claim(id, playerKey);
        if (claim == null) {
            return ResponseEntity.status(409).body(Map.of("error", "mail_not_claimable", "id", id));
        }
        return ResponseEntity.ok(Map.of(
            "claimId", claim.id,
            "mailId", claim.mailId,
            "playerKey", claim.playerKey,
            "attachments", claim.claimedAttachments == null ? "" : claim.claimedAttachments,
            "claimedAt", claim.claimedAt.toString(),
            "duplicate", claim.claimedAt.isBefore(java.time.LocalDateTime.now().minusSeconds(5))));
    }

    private String currentOperator() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        return auth != null ? auth.getName() : "api";
    }
}
