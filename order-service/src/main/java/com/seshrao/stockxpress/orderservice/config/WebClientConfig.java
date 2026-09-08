package com.seshrao.stockxpress.orderservice.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * WebClient configuration with proper timeouts, retry, and error handling.
 * Configures connection pooling and timeout settings for optimal performance.
 */
@Configuration
@Slf4j
public class WebClientConfig {

    private static final int CONNECTION_TIMEOUT_MS = 5000;      // 5 seconds
    private static final int READ_TIMEOUT_SECONDS = 10;         // 10 seconds
    private static final int WRITE_TIMEOUT_SECONDS = 10;        // 10 seconds
    private static final int MAX_CONNECTIONS = 500;             // Maximum connections in pool
    private static final int PENDING_ACQUIRE_TIMEOUT_MS = 45000; // 45 seconds
    private static final Duration MAX_IDLE_TIME = Duration.ofSeconds(20);
    private static final Duration MAX_LIFE_TIME = Duration.ofMinutes(5);

    /**
     * Creates a load-balanced WebClient.Builder with custom HTTP client configuration.
     * Includes connection pooling, timeouts, and logging filters.
     */
    @Bean
    @LoadBalanced // Enable client-side load balancing
    public WebClient.Builder webClientBuilder() {
        // Configure connection provider with pooling
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(MAX_CONNECTIONS)
                .maxIdleTime(MAX_IDLE_TIME)
                .maxLifeTime(MAX_LIFE_TIME)
                .pendingAcquireTimeout(Duration.ofMillis(PENDING_ACQUIRE_TIMEOUT_MS))
                .evictInBackground(Duration.ofSeconds(120))
                .build();

        // Configure HTTP client with timeouts
        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, CONNECTION_TIMEOUT_MS)
                .doOnConnected(connection -> connection
                        .addHandlerLast(new ReadTimeoutHandler(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)))
                .responseTimeout(Duration.ofSeconds(READ_TIMEOUT_SECONDS));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(logResponse())
                .filter(errorHandler());
    }

    /**
     * Logs outgoing requests for debugging and monitoring.
     */
    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) ->
                    values.forEach(value -> log.debug("{}={}", name, value)));
            return Mono.just(clientRequest);
        });
    }

    /**
     * Logs incoming responses for debugging and monitoring.
     */
    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.debug("Response status: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }

    /**
     * Handles errors in WebClient requests with proper logging.
     */
    private ExchangeFilterFunction errorHandler() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            if (clientResponse.statusCode().isError()) {
                log.error("Error response: {} - {}",
                        clientResponse.statusCode().value(),
                        clientResponse.statusCode().getReasonPhrase());
            }
            return Mono.just(clientResponse);
        });
    }
}
