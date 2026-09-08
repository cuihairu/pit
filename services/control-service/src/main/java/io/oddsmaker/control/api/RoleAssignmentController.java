package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.RoleEntity;
import io.oddsmaker.control.jpa.UserRoleEntity;
import io.oddsmaker.control.security.AccessGuard;
import io.oddsmaker.control.service.AuditLogService;
import io.oddsmaker.control.service.PermissionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 公司内 RBAC 角色分配 API：global/game/environment 三级 scope。
 * 角色集合：owner/operator/analyst/developer/risk_admin/viewer。
 */
@RestController
@RequestMapping("/api/users/{userId}/role-assignments")
public class RoleAssignmentController {

    private static final List<String> ASSIGNABLE_ROLES =
        List.of("owner", "operator", "analyst", "developer", "risk_admin", "viewer");

    private final PermissionService permissionService;
    private final AccessGuard accessGuard;
    private final AuditLogService auditLog;

    public RoleAssignmentController(PermissionService permissionService,
                                    AccessGuard accessGuard,
                                    AuditLogService auditLog) {
        this.permissionService = permissionService;
        this.accessGuard = accessGuard;
        this.auditLog = auditLog;
    }

    public static class AssignReq {
        public String roleId;
        public String gameId;       // null = global
        public String environment;  // null = game/global
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> list(@PathVariable String userId) {
        accessGuard.requirePermission("user:read");
        return ResponseEntity.ok(permissionService.listAssignments(userId));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> assign(@PathVariable String userId,
                                                      @RequestBody AssignReq req) {
        accessGuard.requirePermission("user:update");
        validateScope(req);
        UserRoleEntity assignment = permissionService.assignRole(
            userId, req.roleId, req.gameId, req.environment, currentOperator());
        auditLog.logPermissionChange(currentOperator(), currentOperator(), userId,
            userId, "GRANT_ROLE", null,
            "roleId=" + req.roleId + ",gameId=" + req.gameId + ",environment=" + req.environment, null);
        return ResponseEntity.ok(toResp(assignment));
    }

    @DeleteMapping
    public ResponseEntity<Map<String, Object>> revoke(@PathVariable String userId,
                                                      @RequestParam String roleId,
                                                      @RequestParam(required = false) String gameId,
                                                      @RequestParam(required = false) String environment) {
        accessGuard.requirePermission("user:update");
        permissionService.revokeRole(userId, roleId, gameId, environment);
        auditLog.logPermissionChange(currentOperator(), currentOperator(), userId,
            userId, "REVOKE_ROLE",
            "roleId=" + roleId + ",gameId=" + gameId + ",environment=" + environment, null, null);
        return ResponseEntity.ok(Map.of("revoked", true, "userId", userId, "roleId", roleId));
    }

    private void validateScope(AssignReq req) {
        if (req.roleId == null || req.roleId.isBlank()) {
            throw new IllegalArgumentException("roleId is required");
        }
        if (!ASSIGNABLE_ROLES.contains(req.roleId)) {
            throw new IllegalArgumentException(
                "Non-assignable role: " + req.roleId + " (expected one of " + ASSIGNABLE_ROLES + ")");
        }
        if (req.environment != null && req.gameId == null) {
            throw new IllegalArgumentException("environment scope requires gameId");
        }
    }

    private String currentOperator() {
        var auth = org.springframework.security.core.context.SecurityContextHolder
            .getContext().getAuthentication();
        return auth != null ? auth.getName() : "api";
    }

    private static Map<String, Object> toResp(UserRoleEntity assignment) {
        return Map.of(
            "userId", assignment.userId,
            "roleId", assignment.roleId,
            "gameId", assignment.gameId == null ? "" : assignment.gameId,
            "environment", assignment.environment == null ? "" : assignment.environment,
            "enabled", assignment.isEnabled());
    }
}
