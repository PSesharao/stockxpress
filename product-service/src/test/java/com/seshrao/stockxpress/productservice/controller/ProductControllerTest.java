package com.seshrao.stockxpress.productservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.seshrao.stockxpress.productservice.dto.ProductRequest;
import com.seshrao.stockxpress.productservice.dto.ProductResponse;
import com.seshrao.stockxpress.productservice.service.ProductService;
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
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Unit tests for ProductController.
 * Tests REST endpoints for product creation and retrieval.
 */
@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductRequest productRequest;
    private ProductResponse productResponse;

    @BeforeEach
    void setUp() {
        // Given - Setup test data
        productRequest = ProductRequest.builder()
                .name("iPhone 13")
                .description("Latest iPhone model")
                .price(new BigDecimal("999.99"))
                .build();

        productResponse = ProductResponse.builder()
                .id("507f1f77bcf86cd799439011")
                .name("iPhone 13")
                .description("Latest iPhone model")
                .price(new BigDecimal("999.99"))
                .build();
    }

    /**
     * Test creating a product successfully.
     * Given: Valid product request
     * When: POST /api/product is called
     * Then: Product is created and HTTP 201 CREATED is returned
     */
    @Test
    @DisplayName("Should create product successfully and return 201 CREATED")
    void shouldCreateProductSuccessfully() throws Exception {
        // Given
        doNothing().when(productService).createProduct(any(ProductRequest.class));

        // When & Then
        mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().isCreated());

        verify(productService, times(1)).createProduct(any(ProductRequest.class));
    }

    /**
     * Test creating a product with null name.
     * Given: Product request with null name
     * When: POST /api/product is called
     * Then: Product creation is attempted (no validation in current implementation)
     */
    @Test
    @DisplayName("Should handle product creation with null name")
    void shouldHandleProductCreationWithNullName() throws Exception {
        // Given
        ProductRequest invalidRequest = ProductRequest.builder()
                .name(null)
                .description("Test description")
                .price(new BigDecimal("100.00"))
                .build();

        doNothing().when(productService).createProduct(any(ProductRequest.class));

        // When & Then
        mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isCreated());

        verify(productService, times(1)).createProduct(any(ProductRequest.class));
    }

    /**
     * Test creating a product with negative price.
     * Given: Product request with negative price
     * When: POST /api/product is called
     * Then: Product creation is attempted (no validation in current implementation)
     */
    @Test
    @DisplayName("Should handle product creation with negative price")
    void shouldHandleProductCreationWithNegativePrice() throws Exception {
        // Given
        ProductRequest invalidRequest = ProductRequest.builder()
                .name("Test Product")
                .description("Test description")
                .price(new BigDecimal("-50.00"))
                .build();

        doNothing().when(productService).createProduct(any(ProductRequest.class));

        // When & Then
        mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isCreated());

        verify(productService, times(1)).createProduct(any(ProductRequest.class));
    }

    /**
     * Test retrieving all products successfully.
     * Given: Products exist in the system
     * When: GET /api/product is called
     * Then: List of products is returned with HTTP 200 OK
     */
    @Test
    @DisplayName("Should retrieve all products successfully and return 200 OK")
    void shouldGetAllProductsSuccessfully() throws Exception {
        // Given
        ProductResponse productResponse2 = ProductResponse.builder()
                .id("507f1f77bcf86cd799439012")
                .name("MacBook Pro")
                .description("Latest MacBook model")
                .price(new BigDecimal("2499.99"))
                .build();

        List<ProductResponse> productList = Arrays.asList(productResponse, productResponse2);
        when(productService.getAllProducts()).thenReturn(productList);

        // When & Then
        mockMvc.perform(get("/api/product")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value("507f1f77bcf86cd799439011"))
                .andExpect(jsonPath("$[0].name").value("iPhone 13"))
                .andExpect(jsonPath("$[0].price").value(999.99))
                .andExpect(jsonPath("$[1].id").value("507f1f77bcf86cd799439012"))
                .andExpect(jsonPath("$[1].name").value("MacBook Pro"))
                .andExpect(jsonPath("$[1].price").value(2499.99));

        verify(productService, times(1)).getAllProducts();
    }

    /**
     * Test retrieving all products when no products exist.
     * Given: No products exist in the system
     * When: GET /api/product is called
     * Then: Empty list is returned with HTTP 200 OK
     */
    @Test
    @DisplayName("Should return empty list when no products exist")
    void shouldReturnEmptyListWhenNoProductsExist() throws Exception {
        // Given
        when(productService.getAllProducts()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/product")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(productService, times(1)).getAllProducts();
    }

    /**
     * Test creating a product with malformed JSON.
     * Given: Invalid JSON request body
     * When: POST /api/product is called
     * Then: HTTP 400 BAD REQUEST is returned
     */
    @Test
    @DisplayName("Should return 400 BAD REQUEST for malformed JSON")
    void shouldReturnBadRequestForMalformedJson() throws Exception {
        // Given
        String malformedJson = "{name: 'iPhone', invalid}";

        // When & Then
        mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(malformedJson))
                .andExpect(status().isBadRequest());

        verify(productService, never()).createProduct(any(ProductRequest.class));
    }

    /**
     * Test service layer exception handling during product creation.
     * Given: Service throws RuntimeException
     * When: POST /api/product is called
     * Then: Exception is propagated
     */
    @Test
    @DisplayName("Should propagate exception when service throws error during creation")
    void shouldPropagateExceptionDuringCreation() throws Exception {
        // Given
        doThrow(new RuntimeException("Database connection failed"))
                .when(productService).createProduct(any(ProductRequest.class));

        // When & Then
        mockMvc.perform(post("/api/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(productRequest)))
                .andExpect(status().is5xxServerError());

        verify(productService, times(1)).createProduct(any(ProductRequest.class));
    }

    /**
     * Test service layer exception handling during product retrieval.
     * Given: Service throws RuntimeException
     * When: GET /api/product is called
     * Then: Exception is propagated
     */
    @Test
    @DisplayName("Should propagate exception when service throws error during retrieval")
    void shouldPropagateExceptionDuringRetrieval() throws Exception {
        // Given
        when(productService.getAllProducts())
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(get("/api/product")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

        verify(productService, times(1)).getAllProducts();
    }
}
