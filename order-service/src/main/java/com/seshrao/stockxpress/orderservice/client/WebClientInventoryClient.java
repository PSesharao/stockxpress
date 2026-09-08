package com.seshrao.stockxpress.orderservice.client;

import com.seshrao.stockxpress.common.exception.ServiceUnavailableException;
import com.seshrao.stockxpress.orderservice.dto.InventoryResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * WebClient-based implementation of InventoryClient.
 * Handles communication with the Inventory Service with proper error handling,
 * timeouts, retries, and circuit breaker pattern.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebClientInventoryClient implements InventoryClient {

    private static final String INVENTORY_SERVICE_URL = "http://inventory-service/api/inventory";
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(3);
    private static final String CIRCUIT_BREAKER_NAME = "inventoryService";
    
    private final WebClient.Builder webClientBuilder;
    private final Tracer tracer;

    /**
     * Checks inventory availability with circuit breaker and retry mechanisms.
     * Falls back to empty list if service is unavailable.
     */
    @Override
    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "inventoryServiceFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    public List<InventoryResponse> checkInventory(List<String> skuCodes) {
        log.info("Checking inventory for SKU codes: {}", skuCodes);
        
        Span inventoryServiceLookup = tracer.nextSpan().name("InventoryServiceLookup");
        
        try (Tracer.SpanInScope isLookup = tracer.withSpan(inventoryServiceLookup.start())) {
            InventoryResponse[] inventoryResponses = webClientBuilder.build()
                    .get()
                    .uri(INVENTORY_SERVICE_URL, uriBuilder -> 
                        uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .timeout(REQUEST_TIMEOUT)
                    .onErrorResume(WebClientResponseException.class, this::handleWebClientError)
                    .block();
            
            List<InventoryResponse> responseList = inventoryResponses != null 
                    ? Arrays.asList(inventoryResponses) 
                    : Collections.emptyList();
            
            log.info("Inventory check completed. Found {} items", responseList.size());
            return responseList;
            
        } catch (Exception e) {
            log.error("Error checking inventory for SKU codes: {}", skuCodes, e);
            throw new ServiceUnavailableException(
                "Inventory service is currently unavailable. Please try again later.", e);
        } finally {
            inventoryServiceLookup.end();
        }
    }

    /**
     * Fallback method when circuit breaker is open or retries are exhausted.
     */
    private List<InventoryResponse> inventoryServiceFallback(List<String> skuCodes, Exception e) {
        log.warn("Inventory service fallback triggered for SKU codes: {}. Reason: {}", 
                skuCodes, e.getMessage());
        throw new ServiceUnavailableException(
            "Inventory service is temporarily unavailable. Please try again later.", e);
    }

    /**
     * Handles WebClient-specific errors.
     */
    private Mono<InventoryResponse[]> handleWebClientError(WebClientResponseException e) {
        log.error("WebClient error: Status={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
        return Mono.error(new ServiceUnavailableException(
            "Failed to communicate with inventory service: " + e.getMessage(), e));
    }
}
