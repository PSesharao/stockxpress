package com.seshrao.stockxpress.orderservice.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Resilience4j configuration for circuit breaker, retry, and time limiter.
 * Provides resilience patterns for external service calls.
 */
@Configuration
public class Resilience4jConfig {

    /**
     * Circuit breaker configuration for inventory service.
     * Opens circuit after 50% failure rate in 100 calls.
     */
    @Bean
    public CircuitBreakerConfig inventoryServiceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50)                          // Open circuit at 50% failure rate
                .waitDurationInOpenState(Duration.ofSeconds(30))    // Wait 30s before trying half-open
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(100)                            // Count last 100 calls
                .minimumNumberOfCalls(10)                          // Minimum 10 calls before calculating
                .permittedNumberOfCallsInHalfOpenState(5)          // Allow 5 test calls in half-open
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
    }

    /**
     * Retry configuration for inventory service.
     * Retries up to 3 times with exponential backoff.
     */
    @Bean
    public RetryConfig inventoryServiceRetryConfig() {
        return RetryConfig.custom()
                .maxAttempts(3)                                    // Maximum 3 attempts (1 original + 2 retries)
                .waitDuration(Duration.ofMillis(500))              // Wait 500ms between retries
                .retryExceptions(Exception.class)                  // Retry on any exception
                .build();
    }

    /**
     * Time limiter configuration to prevent long-running operations.
     */
    @Bean
    public TimeLimiterConfig inventoryServiceTimeLimiterConfig() {
        return TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(5))            // 5 seconds timeout
                .cancelRunningFuture(true)
                .build();
    }
}
