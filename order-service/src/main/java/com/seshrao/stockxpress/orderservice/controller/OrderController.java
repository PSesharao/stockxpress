package com.seshrao.stockxpress.orderservice.controller;

import com.seshrao.stockxpress.orderservice.dto.OrderRequest;
import com.seshrao.stockxpress.orderservice.service.OrderService;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.concurrent.CompletableFuture;

/**
 * REST Controller for Order Management.
 * Handles order placement with resilience patterns (Circuit Breaker, Retry, Time Limiter).
 *
 * @author StockXpress Team
 */
@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    /**
     * Place a new order with resilience4j patterns.
     * Uses Circuit Breaker, Time Limiter, and Retry for fault tolerance.
     *
     * @param orderRequest validated order request
     * @return CompletableFuture with order placement result
     */
    @PostMapping
    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    @TimeLimiter(name = "inventory")
    @Retry(name = "inventory")
    public CompletableFuture<ResponseEntity<String>> placeOrder(@Valid @RequestBody OrderRequest orderRequest) {
        return CompletableFuture.supplyAsync(() -> {
            String result = orderService.placeOrder(orderRequest);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        });
    }

    /**
     * Fallback method for order placement when circuit breaker is triggered.
     *
     * @param orderRequest     the order request
     * @param runtimeException the exception that triggered the fallback
     * @return CompletableFuture with error message
     */
    public CompletableFuture<ResponseEntity<String>> fallbackMethod(OrderRequest orderRequest, RuntimeException runtimeException) {
        return CompletableFuture.supplyAsync(() ->
                ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Oops! Something went wrong, please order after some time! " + runtimeException.getMessage())
        );
    }

}
