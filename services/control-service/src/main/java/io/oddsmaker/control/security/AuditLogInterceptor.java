package io.oddsmaker.control.security;

import io.oddsmaker.control.jpa.AuditLogEntity;
import io.oddsmaker.control.service.AuditLogService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 审计日志拦截器
 * 自动记录API操作的审计日志
 */
@Component
public class AuditLogInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AuditLogInterceptor.class);

    @Autowired
    private AuditLogService auditLogService;

    private static final String START_TIME_ATTR = "auditStartTime";
    private static final String REQUEST_ID_ATTR = "auditRequestId";

    /**
     * 请求预处理
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 记录开始时间
        long startTime = System.currentTimeMillis();
        request.setAttribute(START_TIME_ATTR, startTime);
        
        // 生成请求ID
        String requestId = UUID.randomUUID().toString();
        request.setAttribute(REQUEST_ID_ATTR, requestId);
        
        return true;
    }

    /**
     * 请求后处理
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
            Object handler, Exception ex) {
        
        // 只处理控制器方法
        if (!(handler instanceof HandlerMethod)) {
            return;
        }

        HandlerMethod handlerMethod = (HandlerMethod) handler;
        
        // 检查是否有审计注解
        Auditable auditable = handlerMethod.getMethod().getAnnotation(Auditable.class);
        if (auditable == null) {
            return;
        }

        try {
            // 计算处理时间
            long startTime = (Long) request.getAttribute(START_TIME_ATTR);
            long duration = System.currentTimeMillis() - startTime;
            String requestId = (String) request.getAttribute(REQUEST_ID_ATTR);

            // 获取当前用户
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String userId = null;
            String username = null;
            
            if (authentication != null && authentication.isAuthenticated()) {
                username = authentication.getName();
                // 这里可以进一步获取用户ID
            }

            // 创建审计日志
            AuditLogEntity auditLog = new AuditLogEntity();
            auditLog.userId = userId;
            auditLog.username = username;
            auditLog.action = auditable.action();
            auditLog.resourceType = auditable.resourceType();
            auditLog.ipAddress = getClientIp(request);
            auditLog.userAgent = request.getHeader("User-Agent");
            auditLog.requestId = requestId;
            auditLog.durationMs = duration;
            auditLog.status = response.getStatus() < 400 ? 
                AuditLogEntity.AuditStatus.SUCCESS : AuditLogEntity.AuditStatus.FAILURE;

            // 从请求中提取资源信息
            extractResourceInfo(request, handlerMethod, auditLog);

            // 记录审计日志
            auditLogService.log(auditLog);
            
            logger.debug("Audit log recorded: {} - {} - {} - {}ms", 
                auditLog.action, auditLog.resourceType, auditLog.resourceId, duration);
                
        } catch (Exception e) {
            logger.error("Failed to record audit log", e);
        }
    }

    /**
     * 从请求中提取资源信息
     */
    private void extractResourceInfo(HttpServletRequest request, HandlerMethod handlerMethod, 
            AuditLogEntity auditLog) {
        
        // 从路径变量中提取资源ID
        String uri = request.getRequestURI();
        String[] parts = uri.split("/");
        
        // 尝试从路径中提取资源ID
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("api") && i + 2 < parts.length) {
                auditLog.resourceType = parts[i + 1];
                if (i + 2 < parts.length) {
                    auditLog.resourceId = parts[i + 2];
                }
                break;
            }
        }

        // 从请求参数中提取游戏ID和环境
        String gameId = request.getParameter("gameId");
        if (gameId != null) {
            auditLog.gameId = gameId;
        }

        String environment = request.getParameter("environment");
        if (environment != null) {
            auditLog.environment = environment;
        }
    }

    /**
     * 获取客户端IP地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            // 多个代理时取第一个
            return ip.split(",")[0].trim();
        }
        
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        ip = request.getHeader("Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        
        return request.getRemoteAddr();
    }
}