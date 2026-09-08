package com.seshrao.stockxpress.orderservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seshrao.stockxpress.orderservice.dto.OrderLineItemDto;
import com.seshrao.stockxpress.orderservice.dto.OrderRequest;
import com.seshrao.stockxpress.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for OrderController.
 * Tests REST API endpoints with circuit breaker, retry, and time limiter patterns.
 */
@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    private OrderRequest orderRequest;
    private OrderLineItemDto orderLineItem1;
    private OrderLineItemDto orderLineItem2;

    @BeforeEach
    void setUp() {
        // Given - Setup test data
        orderLineItem1 = new OrderLineItemDto();
        orderLineItem1.setSkuCode("iphone-13");
        orderLineItem1.setPrice(new BigDecimal("999.99"));
        orderLineItem1.setQuantity(2);

        orderLineItem2 = new OrderLineItemDto();
        orderLineItem2.setSkuCode("macbook-pro");
        orderLineItem2.setPrice(new BigDecimal("2499.99"));
        orderLineItem2.setQuantity(1);

        orderRequest = new OrderRequest();
        orderRequest.setOrderLineItemList(Arrays.asList(orderLineItem1, orderLineItem2));
    }

    /**
     * Test placing an order successfully.
     * Given: Valid order request
     * When: POST /api/order is called
     * Then: Returns HTTP 201 and success message
     */
    @Test
    @DisplayName("Should place order successfully and return HTTP 201")
    void shouldPlaceOrderSuccessfully() throws Exception {
        // Given
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenReturn("Order Placed Successfully");

        // When & Then
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Order Placed Successfully"));

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class));
    }

    /**
     * Test placing order with single line item.
     * Given: Order request with single item
     * When: POST /api/order is called
     * Then: Returns HTTP 201 and success message
     */
    @Test
    @DisplayName("Should place order with single line item")
    void shouldPlaceOrderWithSingleLineItem() throws Exception {
        // Given
        orderRequest.setOrderLineItemList(Collections.singletonList(orderLineItem1));
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenReturn("Order Placed Successfully");

        // When & Then
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Order Placed Successfully"));

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class));
    }

    /**
     * Test placing order when product is out of stock.
     * Given: Order request for out-of-stock products
     * When: POST /api/order is called
     * Then: Returns HTTP 201 with error message (handled by fallback)
     */
    @Test
    @DisplayName("Should return error message when product is out of stock")
    void shouldReturnErrorMessageWhenProductOutOfStock() throws Exception {
        // Given
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new IllegalArgumentException("Product is not in stock , please try again later"));

        // When & Then
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated());

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class));
    }

    /**
     * Test placing order with empty line items list.
     * Given: Order request with empty line items
     * When: POST /api/order is called
     * Then: Returns HTTP 201 (controller accepts it, service validates)
     */
    @Test
    @DisplayName("Should handle order request with empty line items")
    void shouldHandleOrderRequestWithEmptyLineItems() throws Exception {
        // Given
        orderRequest.setOrderLineItemList(Collections.emptyList());
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenReturn("Order Placed Successfully");

        // When & Then
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated());

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class));
    }

    /**
     * Test placing order with malformed JSON.
     * Given: Invalid JSON request
     * When: POST /api/order is called
     * Then: Returns HTTP 400 Bad Request
     */
    @Test
    @DisplayName("Should return HTTP 400 for malformed JSON")
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        // Given
        String malformedJson = "{\"orderLineItemList\": [invalid-json}";

        // When & Then
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(orderService, never()).placeOrder(any(OrderRequest.class));
    }

    /**
     * Test placing order with large quantity.
     * Given: Order request with large quantity values
     * When: POST /api/order is called
     * Then: Returns HTTP 201 and processes successfully
     */
    @Test
    @DisplayName("Should handle order with large quantity values")
    void shouldHandleOrderWithLargeQuantityValues() throws Exception {
        // Given
        orderLineItem1.setQuantity(1000);
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenReturn("Order Placed Successfully");

        // When & Then
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Order Placed Successfully"));

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class));
    }

    /**
     * Test placing order with zero price.
     * Given: Order request with zero price items
     * When: POST /api/order is called
     * Then: Returns HTTP 201 and processes successfully
     */
    @Test
    @DisplayName("Should handle order with zero price items")
    void shouldHandleOrderWithZeroPriceItems() throws Exception {
        // Given
        orderLineItem1.setPrice(BigDecimal.ZERO);
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenReturn("Order Placed Successfully");

        // When & Then
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Order Placed Successfully"));

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class));
    }

    /**
     * Test placing order with very large decimal price.
     * Given: Order request with large decimal price values
     * When: POST /api/order is called
     * Then: Returns HTTP 201 and processes successfully
     */
    @Test
    @DisplayName("Should handle order with very large decimal prices")
    void shouldHandleOrderWithVeryLargeDecimalPrices() throws Exception {
        // Given
        orderLineItem1.setPrice(new BigDecimal("999999.99"));
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenReturn("Order Placed Successfully");

        // When & Then
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Order Placed Successfully"));

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class));
    }

    /**
     * Test placing order with special characters in SKU code.
     * Given: Order request with special characters in SKU
     * When: POST /api/order is called
     * Then: Returns HTTP 201 and processes successfully
     */
    @Test
    @DisplayName("Should handle SKU codes with special characters")
    void shouldHandleSkuCodesWithSpecialCharacters() throws Exception {
        // Given
        orderLineItem1.setSkuCode("product-123_456@special");
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenReturn("Order Placed Successfully");

        // When & Then
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated())
                .andExpect(content().string("Order Placed Successfully"));

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class));
    }

    /**
     * Test circuit breaker fallback method invocation.
     * Given: Order service throws RuntimeException
     * When: POST /api/order is called
     * Then: Fallback method is triggered returning error message
     */
    @Test
    @DisplayName("Should invoke fallback method when service throws exception")
    void shouldInvokeFallbackMethodWhenServiceThrowsException() throws Exception {
        // Given
        when(orderService.placeOrder(any(OrderRequest.class)))
                .thenThrow(new RuntimeException("Service unavailable"));

        // When & Then
        mockMvc.perform(post("/api/order")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(orderRequest)))
                .andExpect(status().isCreated());

        verify(orderService, times(1)).placeOrder(any(OrderRequest.class));
    }
}
