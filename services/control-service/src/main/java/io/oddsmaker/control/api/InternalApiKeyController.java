package io.oddsmaker.control.api;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Gateway 内部凭据接口。
 *
 * 该路径由 AdminTokenFilter 的服务间令牌分支保护，禁止用于浏览器或 SDK 调用。
 */
@RestController
@RequestMapping("/internal/api-keys")
public class InternalApiKeyController {
    private final ControlService controlService;

    public InternalApiKeyController(ControlService controlService) {
        this.controlService = controlService;
    }

    @GetMapping("/{apiKey}")
    public ResponseEntity<Models.InternalApiKeyResp> getActiveKey(@PathVariable String apiKey) {
        Models.InternalApiKeyResp key = controlService.getActiveKeyForGateway(apiKey);
        return key == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(key);
    }
}
