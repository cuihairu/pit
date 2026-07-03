package io.oddsmaker.control.security;

import java.lang.annotation.*;

/**
 * 权限检查注解
 * 用于在控制器方法上进行权限检查
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequirePermission {
    
    /**
     * 权限ID
     */
    String value();
    
    /**
     * 游戏ID参数名（可选）
     */
    String gameIdParam() default "";
    
    /**
     * 环境参数名（可选）
     */
    String environmentParam() default "";
}