package com.seshrao.stockxpress.inventoryservice.controller;

import com.seshrao.stockxpress.inventoryservice.dto.InventoryResponse;
import com.seshrao.stockxpress.inventoryservice.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Comprehensive test suite for InventoryController.
 * Tests REST API endpoints using MockMvc for HTTP interactions.
 * Covers happy paths, edge cases, multiple SKU codes, and error scenarios.
 */
@WebMvcTest(InventoryController.class)
@DisplayName("InventoryController Tests")
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InventoryService inventoryService;

    private List<InventoryResponse> mockInventoryResponseList;

    @BeforeEach
    void setUp() {
        // Setup common test data
        mockInventoryResponseList = Arrays.asList(
                InventoryResponse.builder()
                        .skuCode("Iphone_13")
                        .isInStock(true)
                        .build(),
                InventoryResponse.builder()
                        .skuCode("Iphone_13_red")
                        .isInStock(true)
                        .build()
        );
    }

    /**
     * Test GET /api/inventory with single SKU code.
     * Verifies that the endpoint returns inventory status for a single product.
     * Expected: HTTP 200 OK with inventory response containing SKU code and stock status.
     */
    @Test
    @DisplayName("Should return inventory status for single SKU code")
    void shouldReturnInventoryStatusForSingleSkuCode() throws Exception {
        // Given
        List<InventoryResponse> singleItemResponse = Collections.singletonList(
                InventoryResponse.builder()
                        .skuCode("Iphone_13")
                        .isInStock(true)
                        .build()
        );
        when(inventoryService.isInStock(Arrays.asList("Iphone_13")))
                .thenReturn(singleItemResponse);

        // When & Then
        mockMvc.perform(get("/api/inventory")
                        .param("skuCode", "Iphone_13")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].skuCode", is("Iphone_13")))
                .andExpect(jsonPath("$[0].isInStock", is(true)));

        verify(inventoryService, times(1)).isInStock(Arrays.asList("Iphone_13"));
    }

    /**
     * Test GET /api/inventory with multiple SKU codes.
     * Verifies that the endpoint can handle multiple SKU codes in a single request.
     * Expected: HTTP 200 OK with inventory response for all requested SKU codes.
     */
    @Test
    @DisplayName("Should return inventory status for multiple SKU codes")
    void shouldReturnInventoryStatusForMultipleSkuCodes() throws Exception {
        // Given
        List<String> skuCodes = Arrays.asList("Iphone_13", "Iphone_13_red");
        when(inventoryService.isInStock(skuCodes))
                .thenReturn(mockInventoryResponseList);

        // When & Then
        mockMvc.perform(get("/api/inventory")
                        .param("skuCode", "Iphone_13")
                        .param("skuCode", "Iphone_13_red")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].skuCode", is("Iphone_13")))
                .andExpect(jsonPath("$[0].isInStock", is(true)))
                .andExpect(jsonPath("$[1].skuCode", is("Iphone_13_red")))
                .andExpect(jsonPath("$[1].isInStock", is(true)));

        verify(inventoryService, times(1)).isInStock(skuCodes);
    }

    /**
     * Test GET /api/inventory with products out of stock.
     * Verifies that the endpoint correctly returns false for out-of-stock items.
     * Expected: HTTP 200 OK with isInStock=false for unavailable products.
     */
    @Test
    @DisplayName("Should return out of stock status for unavailable products")
    void shouldReturnOutOfStockStatusForUnavailableProducts() throws Exception {
        // Given
        List<InventoryResponse> outOfStockResponse = Arrays.asList(
                InventoryResponse.builder()
                        .skuCode("Iphone_14")
                        .isInStock(false)
                        .build(),
                InventoryResponse.builder()
                        .skuCode("Iphone_15")
                        .isInStock(false)
                        .build()
        );
        List<String> skuCodes = Arrays.asList("Iphone_14", "Iphone_15");
        when(inventoryService.isInStock(skuCodes))
                .thenReturn(outOfStockResponse);

        // When & Then
        mockMvc.perform(get("/api/inventory")
                        .param("skuCode", "Iphone_14")
                        .param("skuCode", "Iphone_15")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].isInStock", is(false)))
                .andExpect(jsonPath("$[1].isInStock", is(false)));

        verify(inventoryService, times(1)).isInStock(skuCodes);
    }

    /**
     * Test GET /api/inventory with mixed stock availability.
     * Verifies that the endpoint correctly handles mixed scenarios (some in stock, some not).
     * Expected: HTTP 200 OK with accurate stock status for each SKU.
     */
    @Test
    @DisplayName("Should return mixed stock status when some products are in stock and others are not")
    void shouldReturnMixedStockStatusForMixedAvailability() throws Exception {
        // Given
        List<InventoryResponse> mixedResponse = Arrays.asList(
                InventoryResponse.builder()
                        .skuCode("Iphone_13")
                        .isInStock(true)
                        .build(),
                InventoryResponse.builder()
                        .skuCode("Iphone_14")
                        .isInStock(false)
                        .build(),
                InventoryResponse.builder()
                        .skuCode("Iphone_13_red")
                        .isInStock(true)
                        .build()
        );
        List<String> skuCodes = Arrays.asList("Iphone_13", "Iphone_14", "Iphone_13_red");
        when(inventoryService.isInStock(skuCodes))
                .thenReturn(mixedResponse);

        // When & Then
        mockMvc.perform(get("/api/inventory")
                        .param("skuCode", "Iphone_13")
                        .param("skuCode", "Iphone_14")
                        .param("skuCode", "Iphone_13_red")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].isInStock", is(true)))
                .andExpect(jsonPath("$[1].isInStock", is(false)))
                .andExpect(jsonPath("$[2].isInStock", is(true)));

        verify(inventoryService, times(1)).isInStock(skuCodes);
    }

    /**
     * Test GET /api/inventory with empty SKU code list.
     * Verifies that the endpoint handles empty parameter list gracefully.
     * Expected: HTTP 200 OK with empty response list.
     */
    @Test
    @DisplayName("Should return empty list when no SKU codes are provided")
    void shouldReturnEmptyListWhenNoSkuCodesProvided() throws Exception {
        // Given
        when(inventoryService.isInStock(Collections.emptyList()))
                .thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/api/inventory")
                        .param("skuCode", "")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    /**
     * Test GET /api/inventory with large number of SKU codes.
     * Verifies that the endpoint can handle bulk inventory checks efficiently.
     * Expected: HTTP 200 OK with responses for all 10 SKU codes.
     */
    @Test
    @DisplayName("Should handle large number of SKU codes efficiently")
    void shouldHandleLargeNumberOfSkuCodes() throws Exception {
        // Given - Create 10 different SKU codes
        List<String> largeSkuList = Arrays.asList(
                "SKU_001", "SKU_002", "SKU_003", "SKU_004", "SKU_005",
                "SKU_006", "SKU_007", "SKU_008", "SKU_009", "SKU_010"
        );
        
        List<InventoryResponse> largeResponseList = Arrays.asList(
                InventoryResponse.builder().skuCode("SKU_001").isInStock(true).build(),
                InventoryResponse.builder().skuCode("SKU_002").isInStock(true).build(),
                InventoryResponse.builder().skuCode("SKU_003").isInStock(false).build(),
                InventoryResponse.builder().skuCode("SKU_004").isInStock(true).build(),
                InventoryResponse.builder().skuCode("SKU_005").isInStock(false).build(),
                InventoryResponse.builder().skuCode("SKU_006").isInStock(true).build(),
                InventoryResponse.builder().skuCode("SKU_007").isInStock(true).build(),
                InventoryResponse.builder().skuCode("SKU_008").isInStock(false).build(),
                InventoryResponse.builder().skuCode("SKU_009").isInStock(true).build(),
                InventoryResponse.builder().skuCode("SKU_010").isInStock(true).build()
        );

        when(inventoryService.isInStock(largeSkuList))
                .thenReturn(largeResponseList);

        // When & Then
        mockMvc.perform(get("/api/inventory")
                        .param("skuCode", largeSkuList.toArray(new String[0]))
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)));

        verify(inventoryService, times(1)).isInStock(largeSkuList);
    }

    /**
     * Test GET /api/inventory with special characters in SKU codes.
     * Verifies that the endpoint handles SKU codes with special characters.
     * Expected: HTTP 200 OK with proper handling of special characters.
     */
    @Test
    @DisplayName("Should handle SKU codes with special characters")
    void shouldHandleSkuCodesWithSpecialCharacters() throws Exception {
        // Given
        List<String> specialSkuCodes = Arrays.asList("SKU-001", "SKU_002", "SKU.003");
        List<InventoryResponse> specialResponse = Arrays.asList(
                InventoryResponse.builder().skuCode("SKU-001").isInStock(true).build(),
                InventoryResponse.builder().skuCode("SKU_002").isInStock(true).build(),
                InventoryResponse.builder().skuCode("SKU.003").isInStock(false).build()
        );

        when(inventoryService.isInStock(specialSkuCodes))
                .thenReturn(specialResponse);

        // When & Then
        mockMvc.perform(get("/api/inventory")
                        .param("skuCode", "SKU-001")
                        .param("skuCode", "SKU_002")
                        .param("skuCode", "SKU.003")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].skuCode", is("SKU-001")))
                .andExpect(jsonPath("$[1].skuCode", is("SKU_002")))
                .andExpect(jsonPath("$[2].skuCode", is("SKU.003")));

        verify(inventoryService, times(1)).isInStock(specialSkuCodes);
    }

    /**
     * Test GET /api/inventory with duplicate SKU codes.
     * Verifies that the endpoint handles duplicate SKU codes in the request.
     * Expected: HTTP 200 OK, behavior depends on service implementation.
     */
    @Test
    @DisplayName("Should handle duplicate SKU codes in request")
    void shouldHandleDuplicateSkuCodes() throws Exception {
        // Given
        List<String> duplicateSkuCodes = Arrays.asList("Iphone_13", "Iphone_13");
        List<InventoryResponse> response = Collections.singletonList(
                InventoryResponse.builder().skuCode("Iphone_13").isInStock(true).build()
        );

        when(inventoryService.isInStock(anyList()))
                .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/inventory")
                        .param("skuCode", "Iphone_13")
                        .param("skuCode", "Iphone_13")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        verify(inventoryService, times(1)).isInStock(anyList());
    }

    /**
     * Test GET /api/inventory when service throws exception.
     * Verifies that the controller properly propagates service layer exceptions.
     * Expected: Exception is thrown and handled by exception handler.
     */
    @Test
    @DisplayName("Should propagate exception when service layer fails")
    void shouldPropagateExceptionWhenServiceFails() throws Exception {
        // Given
        when(inventoryService.isInStock(anyList()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        mockMvc.perform(get("/api/inventory")
                        .param("skuCode", "Iphone_13")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().is5xxServerError());

        verify(inventoryService, times(1)).isInStock(anyList());
    }
}
