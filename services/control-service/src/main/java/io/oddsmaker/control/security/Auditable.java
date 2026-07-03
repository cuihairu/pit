package io.oddsmaker.control.security;

import io.oddsmaker.control.jpa.AuditLogEntity;

import java.lang.annotation.*;

/**
 * 审计注解
 * 用于标记需要记录审计日志的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Auditable {
    
    /**
     * 操作类型
     */
    AuditLogEntity.AuditAction action();
    
    /**
     * 资源类型
     */
    String resourceType() default "";
    
    /**
     * 是否记录请求参数
     */
    boolean logParameters() default false;
    
    /**
     * 是否记录响应结果
     */
    boolean logResponse() default false;
    
    /**
     * 操作描述
     */
    String description() default "";
}