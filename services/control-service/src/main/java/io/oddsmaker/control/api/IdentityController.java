package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.IdentityEntity;
import io.oddsmaker.control.jpa.IdentityLinkEntity;
import io.oddsmaker.control.service.IdentityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 身份查询 API。
 * 提供按 device/player/user/identifier 反查统一身份，以及查身份的关联（identity_links）。
 * 写入由 Flink IdentityMergeJob → Kafka → IdentityConsumer 自动完成，本接口只读。
 */
@RestController
@RequestMapping("/api/identities")
public class IdentityController {

    @Autowired
    private IdentityService identityService;

    @GetMapping("/{gameId}/{identityId}")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<IdentityEntity> getIdentity(@PathVariable String gameId, @PathVariable String identityId) {
        return ResponseEntity.of(identityService.getIdentity(gameId, identityId));
    }

    @GetMapping("/{gameId}/by-device")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<IdentityEntity> findByDevice(@PathVariable String gameId, @RequestParam String deviceId) {
        return ResponseEntity.of(identityService.findByDevice(gameId, deviceId));
    }

    @GetMapping("/{gameId}/by-player")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<IdentityEntity> findByPlayer(@PathVariable String gameId, @RequestParam String playerId) {
        return ResponseEntity.of(identityService.findByPlayer(gameId, playerId));
    }

    @GetMapping("/{gameId}/by-user")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public List<IdentityEntity> findByUser(@PathVariable String gameId, @RequestParam String userId) {
        return identityService.findByUser(gameId, userId);
    }

    @GetMapping("/{gameId}/by-identifier")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public List<IdentityEntity> findByIdentifier(
            @PathVariable String gameId,
            @RequestParam String type,
            @RequestParam String value) {
        return identityService.findByIdentifier(gameId, type, value);
    }

    @GetMapping("/{identityId}/links")
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public List<IdentityLinkEntity> getLinks(@PathVariable String identityId, @RequestParam String gameId) {
        return identityService.getLinks(identityId);
    }
}
