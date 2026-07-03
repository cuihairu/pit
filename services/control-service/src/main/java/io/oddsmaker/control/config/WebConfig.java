package io.oddsmaker.control.config;

import io.oddsmaker.control.security.AuditLogInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置
 * 注册拦截器和其他Web配置
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuditLogInterceptor auditLogInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册审计日志拦截器
        registry.addInterceptor(auditLogInterceptor)
            .addPathPatterns("/api/**")
            .excludePathPatterns(
                "/api/config/**",
                "/api/health/**",
                "/actuator/**"
            );
    }
}