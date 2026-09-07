package io.oddsmaker.gateway.config;

import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * API Key 校验服务。
 *
 * 动态凭据只从 Control Service 的受服务间令牌保护的内部接口读取。
 * 本地静态 key 仅用于开发和测试，因此没有 game/environment 绑定作用域。
 */
@Component
public class AuthService {
    private final Map<String, String> localSecrets;
    private final WebClient client;
    private final String internalToken;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    static class CacheEntry {
        ApiKeyContext context;
        long expireAt;
    }

    public static class ApiKeyContext {
        public String apiKey;
        public String secret;
        public String gameId;
        public String environment;
        public Boolean canWrite;
        public Boolean requireHmac;
        public Integer rpm;
        public Integer ipRpm;
        public List<String> propsAllowlist;
        public String piiEmail;
        public String piiPhone;
        public String piiIp;
        public List<String> denyKeys;
        public List<String> maskKeys;

        public boolean isScoped() {
            return gameId != null && !gameId.isBlank() && environment != null && !environment.isBlank();
        }

        public boolean allowsWrite() {
            return canWrite == null || canWrite;
        }
    }

    public AuthService(Environment env) {
        this.localSecrets = Binder.get(env).bind("oddsmaker.auth.keys", Map.class).orElse(Map.of());
        String controlUrl = Binder.get(env).bind("oddsmaker.control.url", String.class).orElse(null);
        this.internalToken = Binder.get(env).bind("oddsmaker.control.internal-token", String.class).orElse("");
        this.client = controlUrl == null ? null : WebClient.builder().baseUrl(controlUrl).build();
    }

    public ApiKeyContext getContext(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return null;
        }

        long now = Instant.now().getEpochSecond();
        CacheEntry cached = cache.get(apiKey);
        if (cached != null && cached.expireAt > now) {
            return cached.context;
        }

        ApiKeyContext remote = fetchRemoteContext(apiKey);
        if (remote != null) {
            CacheEntry entry = new CacheEntry();
            entry.context = remote;
            entry.expireAt = now + 60;
            cache.put(apiKey, entry);
            return remote;
        }

        String localSecret = localSecrets.get(apiKey);
        if (localSecret == null) {
            return null;
        }
        ApiKeyContext local = new ApiKeyContext();
        local.apiKey = apiKey;
        local.secret = localSecret;
        local.canWrite = true;
        return local;
    }

    private ApiKeyContext fetchRemoteContext(String apiKey) {
        if (client == null || internalToken == null || internalToken.isBlank()) {
            return null;
        }
        try {
            return client.get()
                .uri("/internal/api-keys/{apiKey}", apiKey)
                .header("x-internal-token", internalToken)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(ApiKeyContext.class)
                .onErrorResume(error -> Mono.empty())
                .block();
        } catch (Exception ignored) {
            return null;
        }
    }
}
