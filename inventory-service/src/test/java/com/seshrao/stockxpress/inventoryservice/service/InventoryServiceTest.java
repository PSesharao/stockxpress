package com.seshrao.stockxpress.inventoryservice.service;

import com.seshrao.stockxpress.inventoryservice.dto.InventoryResponse;
import com.seshrao.stockxpress.inventoryservice.model.Inventory;
import com.seshrao.stockxpress.inventoryservice.repository.InventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for InventoryService.
 * Tests business logic for inventory stock checking and response mapping.
 */
@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @InjectMocks
    private InventoryService inventoryService;

    private Inventory inventoryInStock;
    private Inventory inventoryOutOfStock;
    private Inventory inventoryZeroQuantity;

    @BeforeEach
    void setUp() {
        // Given - Setup test data
        inventoryInStock = Inventory.builder()
                .id(1L)
                .skuCode("iphone-13")
                .quantity(100)
                .build();

        inventoryOutOfStock = Inventory.builder()
                .id(2L)
                .skuCode("macbook-pro")
                .quantity(0)
                .build();

        inventoryZeroQuantity = Inventory.builder()
                .id(3L)
                .skuCode("ipad-air")
                .quantity(0)
                .build();
    }

    /**
     * Test checking stock for single SKU code that is in stock.
     * Given: Single SKU code with available quantity
     * When: isInStock is called
     * Then: Returns InventoryResponse with isInStock = true
     */
    @Test
    @DisplayName("Should return in-stock status for product with available quantity")
    void shouldReturnInStockStatusForProductWithAvailableQuantity() {
        // Given
        List<String> skuCodes = Collections.singletonList("iphone-13");
        when(inventoryRepository.findBySkuCodeIn(skuCodes))
                .thenReturn(Collections.singletonList(inventoryInStock));

        // When
        List<InventoryResponse> result = inventoryService.isInStock(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSkuCode()).isEqualTo("iphone-13");
        assertThat(result.get(0).isInStock()).isTrue();
        verify(inventoryRepository, times(1)).findBySkuCodeIn(skuCodes);
    }

    /**
     * Test checking stock for single SKU code that is out of stock.
     * Given: Single SKU code with zero quantity
     * When: isInStock is called
     * Then: Returns InventoryResponse with isInStock = false
     */
    @Test
    @DisplayName("Should return out-of-stock status for product with zero quantity")
    void shouldReturnOutOfStockStatusForProductWithZeroQuantity() {
        // Given
        List<String> skuCodes = Collections.singletonList("macbook-pro");
        when(inventoryRepository.findBySkuCodeIn(skuCodes))
                .thenReturn(Collections.singletonList(inventoryOutOfStock));

        // When
        List<InventoryResponse> result = inventoryService.isInStock(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSkuCode()).isEqualTo("macbook-pro");
        assertThat(result.get(0).isInStock()).isFalse();
        verify(inventoryRepository, times(1)).findBySkuCodeIn(skuCodes);
    }

    /**
     * Test checking stock for multiple SKU codes with mixed availability.
     * Given: Multiple SKU codes with different stock levels
     * When: isInStock is called
     * Then: Returns correct InventoryResponse for each SKU code
     */
    @Test
    @DisplayName("Should return correct stock status for multiple SKU codes")
    void shouldReturnCorrectStockStatusForMultipleSkuCodes() {
        // Given
        Inventory inventoryHighStock = Inventory.builder()
                .id(4L)
                .skuCode("airpods-pro")
                .quantity(250)
                .build();

        List<String> skuCodes = Arrays.asList("iphone-13", "macbook-pro", "airpods-pro");
        when(inventoryRepository.findBySkuCodeIn(skuCodes))
                .thenReturn(Arrays.asList(inventoryInStock, inventoryOutOfStock, inventoryHighStock));

        // When
        List<InventoryResponse> result = inventoryService.isInStock(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        
        assertThat(result.get(0).getSkuCode()).isEqualTo("iphone-13");
        assertThat(result.get(0).isInStock()).isTrue();
        
        assertThat(result.get(1).getSkuCode()).isEqualTo("macbook-pro");
        assertThat(result.get(1).isInStock()).isFalse();
        
        assertThat(result.get(2).getSkuCode()).isEqualTo("airpods-pro");
        assertThat(result.get(2).isInStock()).isTrue();
        
        verify(inventoryRepository, times(1)).findBySkuCodeIn(skuCodes);
    }

    /**
     * Test checking stock when no inventory found for SKU codes.
     * Given: SKU codes that don't exist in inventory
     * When: isInStock is called
     * Then: Returns empty list
     */
    @Test
    @DisplayName("Should return empty list when no inventory found for SKU codes")
    void shouldReturnEmptyListWhenNoInventoryFoundForSkuCodes() {
        // Given
        List<String> skuCodes = Collections.singletonList("non-existent-sku");
        when(inventoryRepository.findBySkuCodeIn(skuCodes))
                .thenReturn(Collections.emptyList());

        // When
        List<InventoryResponse> result = inventoryService.isInStock(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(inventoryRepository, times(1)).findBySkuCodeIn(skuCodes);
    }

    /**
     * Test checking stock with empty SKU code list.
     * Given: Empty SKU code list
     * When: isInStock is called
     * Then: Returns empty list
     */
    @Test
    @DisplayName("Should return empty list when SKU code list is empty")
    void shouldReturnEmptyListWhenSkuCodeListIsEmpty() {
        // Given
        List<String> skuCodes = Collections.emptyList();
        when(inventoryRepository.findBySkuCodeIn(skuCodes))
                .thenReturn(Collections.emptyList());

        // When
        List<InventoryResponse> result = inventoryService.isInStock(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(inventoryRepository, times(1)).findBySkuCodeIn(skuCodes);
    }

    /**
     * Test checking stock for product with quantity of 1.
     * Given: SKU code with minimum stock quantity (1)
     * When: isInStock is called
     * Then: Returns InventoryResponse with isInStock = true
     */
    @Test
    @DisplayName("Should return in-stock status for product with quantity of 1")
    void shouldReturnInStockStatusForProductWithQuantityOfOne() {
        // Given
        Inventory inventoryMinStock = Inventory.builder()
                .id(5L)
                .skuCode("watch-series-7")
                .quantity(1)
                .build();

        List<String> skuCodes = Collections.singletonList("watch-series-7");
        when(inventoryRepository.findBySkuCodeIn(skuCodes))
                .thenReturn(Collections.singletonList(inventoryMinStock));

        // When
        List<InventoryResponse> result = inventoryService.isInStock(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSkuCode()).isEqualTo("watch-series-7");
        assertThat(result.get(0).isInStock()).isTrue();
        verify(inventoryRepository, times(1)).findBySkuCodeIn(skuCodes);
    }

    /**
     * Test checking stock for product with large quantity.
     * Given: SKU code with very large stock quantity
     * When: isInStock is called
     * Then: Returns InventoryResponse with isInStock = true
     */
    @Test
    @DisplayName("Should return in-stock status for product with large quantity")
    void shouldReturnInStockStatusForProductWithLargeQuantity() {
        // Given
        Inventory inventoryLargeStock = Inventory.builder()
                .id(6L)
                .skuCode("charging-cable")
                .quantity(10000)
                .build();

        List<String> skuCodes = Collections.singletonList("charging-cable");
        when(inventoryRepository.findBySkuCodeIn(skuCodes))
                .thenReturn(Collections.singletonList(inventoryLargeStock));

        // When
        List<InventoryResponse> result = inventoryService.isInStock(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSkuCode()).isEqualTo("charging-cable");
        assertThat(result.get(0).isInStock()).isTrue();
        verify(inventoryRepository, times(1)).findBySkuCodeIn(skuCodes);
    }

    /**
     * Test repository exception handling during stock check.
     * Given: Repository throws exception on findBySkuCodeIn
     * When: isInStock is called
     * Then: Exception is propagated
     */
    @Test
    @DisplayName("Should propagate exception when repository fails during findBySkuCodeIn")
    void shouldPropagateExceptionWhenRepositoryFailsDuringFindBySkuCodeIn() {
        // Given
        List<String> skuCodes = Collections.singletonList("iphone-13");
        when(inventoryRepository.findBySkuCodeIn(skuCodes))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When & Then
        try {
            inventoryService.isInStock(skuCodes);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("Database connection failed");
        }

        verify(inventoryRepository, times(1)).findBySkuCodeIn(skuCodes);
    }

    /**
     * Test mapping inventory to InventoryResponse with correct field values.
     * Given: Inventory object with all fields populated
     * When: Inventory is mapped to InventoryResponse
     * Then: All fields are correctly mapped including stock status
     */
    @Test
    @DisplayName("Should correctly map inventory to InventoryResponse")
    void shouldCorrectlyMapInventoryToInventoryResponse() {
        // Given
        List<String> skuCodes = Collections.singletonList("iphone-13");
        when(inventoryRepository.findBySkuCodeIn(skuCodes))
                .thenReturn(Collections.singletonList(inventoryInStock));

        // When
        List<InventoryResponse> result = inventoryService.isInStock(skuCodes);

        // Then
        assertThat(result).hasSize(1);
        InventoryResponse response = result.get(0);
        assertThat(response.getSkuCode()).isEqualTo(inventoryInStock.getSkuCode());
        assertThat(response.isInStock()).isEqualTo(inventoryInStock.getQuantity() > 0);
    }

    /**
     * Test checking stock with duplicate SKU codes in the list.
     * Given: SKU code list with duplicates
     * When: isInStock is called
     * Then: Returns appropriate responses based on repository behavior
     */
    @Test
    @DisplayName("Should handle duplicate SKU codes in the list")
    void shouldHandleDuplicateSkuCodesInList() {
        // Given
        List<String> skuCodes = Arrays.asList("iphone-13", "iphone-13", "macbook-pro");
        when(inventoryRepository.findBySkuCodeIn(skuCodes))
                .thenReturn(Arrays.asList(inventoryInStock, inventoryOutOfStock));

        // When
        List<InventoryResponse> result = inventoryService.isInStock(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        verify(inventoryRepository, times(1)).findBySkuCodeIn(skuCodes);
    }
}
