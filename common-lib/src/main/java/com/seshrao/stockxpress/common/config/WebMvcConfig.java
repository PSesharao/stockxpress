package com.seshrao.stockxpress.common.config;

import com.seshrao.stockxpress.common.interceptor.MDCLoggingInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC Configuration for registering interceptors.
 * 
 * @author Seshrao
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final MDCLoggingInterceptor mdcLoggingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        log.info("Registering MDC Logging Interceptor");
        registry.addInterceptor(mdcLoggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/actuator/**",
                    "/health/**",
                    "/info/**",
                    "/metrics/**"
                );
        log.info("MDC Logging Interceptor registered successfully");
    }
}