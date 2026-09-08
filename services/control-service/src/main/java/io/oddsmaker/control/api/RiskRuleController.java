package io.oddsmaker.control.api;

import io.oddsmaker.control.jpa.RiskRuleEntity;
import io.oddsmaker.control.service.RiskRuleService;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 风控规则 CRUD API。
 * 响应字段与 web 控制台（RiskRulesView）对齐：type/enabled 为投影字段。
 */
@RestController
@RequestMapping("/api/risk-rules")
public class RiskRuleController {

    private final RiskRuleService riskRuleService;

    public RiskRuleController(RiskRuleService riskRuleService) {
        this.riskRuleService = riskRuleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('READ_GAME:' + #gameId)")
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(value = "gameId", required = false) String gameId,
            @RequestParam(value = "environmentId", required = false) String environmentId,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "type", required = false) String type,
            @RequestParam(value = "q", required = false) String q,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "50") int size) {
        Page<RiskRuleEntity> result = riskRuleService.list(gameId, environmentId, status, type, q, page, size);
        return ResponseEntity.ok(Map.of(
            "content", result.getContent().stream().map(RiskRuleController::toResp).toList(),
            "totalElements", result.getTotalElements(),
            "page", result.getNumber(),
            "size", result.getSize()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<RiskRuleResp> get(@PathVariable String id) {
        RiskRuleEntity rule = riskRuleService.get(id);
        return rule == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(toResp(rule));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MANAGE_RISK:' + #rule.gameId)")
    public ResponseEntity<RiskRuleResp> create(@RequestBody RiskRuleEntity rule) {
        return ResponseEntity.ok(toResp(riskRuleService.create(rule, "api")));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MANAGE_RISK:' + #gameId)")
    public ResponseEntity<RiskRuleResp> update(@PathVariable String id,
                                               @RequestParam(value = "gameId", required = false) String gameId,
                                               @RequestBody RiskRuleEntity req) {
        RiskRuleEntity updated = riskRuleService.update(id, req, "api");
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(toResp(updated));
    }

    @PostMapping("/{id}/enable")
    public ResponseEntity<RiskRuleResp> enable(@PathVariable String id) {
        RiskRuleEntity updated = riskRuleService.setStatus(id, true, "api");
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(toResp(updated));
    }

    @PostMapping("/{id}/disable")
    public ResponseEntity<RiskRuleResp> disable(@PathVariable String id) {
        RiskRuleEntity updated = riskRuleService.setStatus(id, false, "api");
        return updated == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(toResp(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        return riskRuleService.delete(id, "api")
            ? ResponseEntity.noContent().build()
            : ResponseEntity.notFound().build();
    }

    public static class RiskRuleResp {
        public String id;
        public String gameId;
        public String environmentId;
        public String name;
        public String displayName;
        public String description;
        public String category;
        public String type;
        public String ruleConditions;
        public String riskLevel;
        public Integer riskScore;
        public String actionType;
        public Boolean enableAutoBlock;
        public Integer triggerThreshold;
        public Integer timeWindowMinutes;
        public Integer cooldownMinutes;
        public Integer priority;
        public Boolean enabled;
        public String status;
        public Boolean testMode;
        public Long totalTriggeredCount;
        public java.time.LocalDateTime createdAt;
        public java.time.LocalDateTime updatedAt;
    }

    private static RiskRuleResp toResp(RiskRuleEntity rule) {
        RiskRuleResp out = new RiskRuleResp();
        out.id = rule.id;
        out.gameId = rule.gameId;
        out.environmentId = rule.environmentId;
        out.name = rule.name;
        out.displayName = rule.displayName;
        out.description = rule.description;
        out.category = rule.category == null ? null : rule.category.name();
        out.type = rule.ruleType == null ? null : rule.ruleType.name();
        out.ruleConditions = rule.ruleConditions;
        out.riskLevel = rule.riskLevel == null ? null : rule.riskLevel.name();
        out.riskScore = rule.riskScore;
        out.actionType = rule.actionType == null ? null : rule.actionType.name();
        out.enableAutoBlock = rule.enableAutoBlock;
        out.triggerThreshold = rule.triggerThreshold;
        out.timeWindowMinutes = rule.timeWindowMinutes;
        out.cooldownMinutes = rule.cooldownMinutes;
        out.priority = rule.priority;
        out.enabled = rule.status == RiskRuleEntity.RuleStatus.ACTIVE;
        out.status = rule.status == null ? null : rule.status.name();
        out.testMode = rule.testMode;
        out.totalTriggeredCount = rule.totalTriggeredCount;
        out.createdAt = rule.createdAt;
        out.updatedAt = rule.updatedAt;
        return out;
    }
}
