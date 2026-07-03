package io.oddsmaker.control.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Admin Token过滤器
 * 支持x-admin-token头认证，作为Keycloak OAuth2认证的备用方案
 */
@Component
public class AdminTokenFilter extends OncePerRequestFilter {
    private final String token;

    public AdminTokenFilter(Environment env) {
        this.token = Binder.get(env).bind("oddsmaker.admin.token", String.class).orElse("");
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
        String path = request.getRequestURI();
        if (path.startsWith("/api/config/")) {
            return true;
        }
        // 只处理API请求
        return !(path.startsWith("/api/"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 如果已经有OAuth2认证，跳过Admin Token检查
        if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken) {
            filterChain.doFilter(request, response);
            return;
        }

        // 如果没有配置Admin Token，开发模式自动认证
        if (token == null || token.isEmpty()) {
            // 开发模式 - 设置匿名认证
            var auth = new UsernamePasswordAuthenticationToken(
                "dev-admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        // 检查x-admin-token头
        String hdr = request.getHeader("x-admin-token");
        if (hdr != null && hdr.equals(token)) {
            // 有效的Admin Token - 设置认证
            var auth = new UsernamePasswordAuthenticationToken(
                "admin", null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
            filterChain.doFilter(request, response);
            return;
        }

        // 没有有效的认证
        response.setStatus(401);
        response.setContentType("application/json");
        String json = "{\"code\":\"unauthorized\",\"message\":\"missing_or_invalid_admin_token\"}";
        response.getOutputStream().write(json.getBytes(StandardCharsets.UTF_8));
    }
}
