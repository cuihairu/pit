package io.oddsmaker.control.security;

import io.oddsmaker.control.service.PermissionService;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;

import java.lang.reflect.Parameter;

/**
 * 权限检查切面
 * 处理@RequirePermission注解
 */
@Aspect
@Component
public class PermissionAspect {

    private static final Logger logger = LoggerFactory.getLogger(PermissionAspect.class);

    @Autowired
    private PermissionService permissionService;

    /**
     * 处理权限检查注解
     */
    @Around("@annotation(io.oddsmaker.control.security.RequirePermission)")
    public Object checkPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        
        // 获取权限检查注解
        RequirePermission annotation = signature.getMethod().getAnnotation(RequirePermission.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        // 获取当前认证用户
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new SecurityException("User not authenticated");
        }

        String userId = authentication.getName();
        String permissionId = annotation.value();
        String gameIdParam = annotation.gameIdParam();
        String environmentParam = annotation.environmentParam();

        logger.debug("Checking permission {} for user {}", permissionId, userId);

        // 获取游戏ID和环境参数
        String gameId = null;
        String environment = null;

        if (!gameIdParam.isEmpty()) {
            gameId = extractParameter(signature, joinPoint.getArgs(), gameIdParam);
        }

        if (!environmentParam.isEmpty()) {
            environment = extractParameter(signature, joinPoint.getArgs(), environmentParam);
        }

        // 检查权限
        boolean hasPermission = false;

        if (gameId != null && environment != null) {
            // 检查环境级权限
            hasPermission = permissionService.hasEnvironmentPermission(userId, gameId, environment, permissionId);
        } else if (gameId != null) {
            // 检查游戏级权限
            hasPermission = permissionService.hasGamePermission(userId, gameId, permissionId);
        } else {
            // 检查全局权限
            hasPermission = permissionService.hasPermission(userId, permissionId);
        }

        if (!hasPermission) {
            logger.warn("User {} does not have permission {}", userId, permissionId);
            throw new SecurityException("Access denied: insufficient permissions");
        }

        logger.debug("Permission check passed for user {} with permission {}", userId, permissionId);
        return joinPoint.proceed();
    }

    /**
     * 从方法参数中提取指定名称的参数值
     */
    private String extractParameter(MethodSignature signature, Object[] args, String paramName) {
        Parameter[] parameters = signature.getMethod().getParameters();
        
        for (int i = 0; i < parameters.length; i++) {
            // 检查@PathVariable注解
            PathVariable pathVariable = parameters[i].getAnnotation(PathVariable.class);
            if (pathVariable != null && (pathVariable.value().equals(paramName) || 
                (pathVariable.value().isEmpty() && parameters[i].getName().equals(paramName)))) {
                return args[i] != null ? args[i].toString() : null;
            }
            
            // 检查参数名
            if (parameters[i].getName().equals(paramName)) {
                return args[i] != null ? args[i].toString() : null;
            }
        }
        
        return null;
    }
}