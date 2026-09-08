package com.seshrao.stockxpress.inventoryservice.repository;

import com.seshrao.stockxpress.inventoryservice.model.Inventory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for InventoryRepository.
 * Tests repository operations with in-memory H2 database.
 */
@DataJpaTest
@ActiveProfiles("test")
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Inventory inventory1;
    private Inventory inventory2;
    private Inventory inventory3;

    @BeforeEach
    void setUp() {
        // Clear database before each test
        inventoryRepository.deleteAll();
        entityManager.flush();
        entityManager.clear();

        // Given - Setup test data
        inventory1 = Inventory.builder()
                .skuCode("iphone-13")
                .quantity(100)
                .build();

        inventory2 = Inventory.builder()
                .skuCode("macbook-pro")
                .quantity(50)
                .build();

        inventory3 = Inventory.builder()
                .skuCode("ipad-air")
                .quantity(0)
                .build();

        // Persist test data
        entityManager.persist(inventory1);
        entityManager.persist(inventory2);
        entityManager.persist(inventory3);
        entityManager.flush();
    }

    /**
     * Test finding inventory by single SKU code.
     * Given: Single SKU code exists in database
     * When: findBySkuCodeIn is called
     * Then: Returns list with matching inventory
     */
    @Test
    @DisplayName("Should find inventory by single SKU code")
    void shouldFindInventoryBySingleSkuCode() {
        // Given
        List<String> skuCodes = Collections.singletonList("iphone-13");

        // When
        List<Inventory> result = inventoryRepository.findBySkuCodeIn(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSkuCode()).isEqualTo("iphone-13");
        assertThat(result.get(0).getQuantity()).isEqualTo(100);
    }

    /**
     * Test finding inventory by multiple SKU codes.
     * Given: Multiple SKU codes exist in database
     * When: findBySkuCodeIn is called
     * Then: Returns list with all matching inventory items
     */
    @Test
    @DisplayName("Should find inventory by multiple SKU codes")
    void shouldFindInventoryByMultipleSkuCodes() {
        // Given
        List<String> skuCodes = Arrays.asList("iphone-13", "macbook-pro", "ipad-air");

        // When
        List<Inventory> result = inventoryRepository.findBySkuCodeIn(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(3);
        assertThat(result).extracting(Inventory::getSkuCode)
                .containsExactlyInAnyOrder("iphone-13", "macbook-pro", "ipad-air");
    }

    /**
     * Test finding inventory by non-existent SKU code.
     * Given: SKU code does not exist in database
     * When: findBySkuCodeIn is called
     * Then: Returns empty list
     */
    @Test
    @DisplayName("Should return empty list for non-existent SKU code")
    void shouldReturnEmptyListForNonExistentSkuCode() {
        // Given
        List<String> skuCodes = Collections.singletonList("non-existent-sku");

        // When
        List<Inventory> result = inventoryRepository.findBySkuCodeIn(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    /**
     * Test finding inventory with empty SKU code list.
     * Given: Empty SKU code list
     * When: findBySkuCodeIn is called
     * Then: Returns empty list
     */
    @Test
    @DisplayName("Should return empty list for empty SKU code list")
    void shouldReturnEmptyListForEmptySkuCodeList() {
        // Given
        List<String> skuCodes = Collections.emptyList();

        // When
        List<Inventory> result = inventoryRepository.findBySkuCodeIn(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
    }

    /**
     * Test finding inventory with partial matches.
     * Given: Some SKU codes exist and some don't
     * When: findBySkuCodeIn is called
     * Then: Returns only matching inventory items
     */
    @Test
    @DisplayName("Should return only matching inventory for partial SKU code matches")
    void shouldReturnOnlyMatchingInventoryForPartialSkuCodeMatches() {
        // Given
        List<String> skuCodes = Arrays.asList("iphone-13", "non-existent-1", "macbook-pro", "non-existent-2");

        // When
        List<Inventory> result = inventoryRepository.findBySkuCodeIn(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Inventory::getSkuCode)
                .containsExactlyInAnyOrder("iphone-13", "macbook-pro");
    }

    /**
     * Test finding inventory with duplicate SKU codes in list.
     * Given: SKU code list contains duplicates
     * When: findBySkuCodeIn is called
     * Then: Returns unique inventory items without duplicates
     */
    @Test
    @DisplayName("Should return unique inventory items for duplicate SKU codes")
    void shouldReturnUniqueInventoryItemsForDuplicateSkuCodes() {
        // Given
        List<String> skuCodes = Arrays.asList("iphone-13", "iphone-13", "macbook-pro");

        // When
        List<Inventory> result = inventoryRepository.findBySkuCodeIn(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result).extracting(Inventory::getSkuCode)
                .containsExactlyInAnyOrder("iphone-13", "macbook-pro");
    }

    /**
     * Test finding inventory with case-sensitive SKU codes.
     * Given: SKU codes with different cases
     * When: findBySkuCodeIn is called
     * Then: Returns only exact case matches
     */
    @Test
    @DisplayName("Should perform case-sensitive search for SKU codes")
    void shouldPerformCaseSensitiveSearchForSkuCodes() {
        // Given
        List<String> skuCodes = Arrays.asList("IPHONE-13", "iPhone-13");

        // When
        List<Inventory> result = inventoryRepository.findBySkuCodeIn(skuCodes);

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty(); // No match because actual is "iphone-13" (lowercase)
    }

    /**
     * Test saving inventory and verifying persistence.
     * Given: New inventory object
     * When: save is called
     * Then: Inventory is persisted with generated ID
     */
    @Test
    @DisplayName("Should save inventory successfully")
    void shouldSaveInventorySuccessfully() {
        // Given
        Inventory newInventory = Inventory.builder()
                .skuCode("airpods-pro")
                .quantity(200)
                .build();

        // When
        Inventory savedInventory = inventoryRepository.save(newInventory);
        entityManager.flush();
        entityManager.clear();

        // Then
        assertThat(savedInventory).isNotNull();
        assertThat(savedInventory.getId()).isNotNull();
        assertThat(savedInventory.getSkuCode()).isEqualTo("airpods-pro");
        assertThat(savedInventory.getQuantity()).isEqualTo(200);

        // Verify persistence
        Inventory found = inventoryRepository.findById(savedInventory.getId()).orElse(null);
        assertThat(found).isNotNull();
        assertThat(found.getSkuCode()).isEqualTo("airpods-pro");
    }

    /**
     * Test updating inventory quantity.
     * Given: Existing inventory
     * When: Quantity is updated and saved
     * Then: Inventory is updated in database
     */
    @Test
    @DisplayName("Should update inventory quantity successfully")
    void shouldUpdateInventoryQuantitySuccessfully() {
        // Given
        Inventory existing = inventoryRepository.findBySkuCodeIn(Collections.singletonList("iphone-13")).get(0);
        Long existingId = existing.getId();

        // When
        existing.setQuantity(150);
        inventoryRepository.save(existing);
        entityManager.flush();
        entityManager.clear();

        // Then
        Inventory updated = inventoryRepository.findById(existingId).orElse(null);
        assertThat(updated).isNotNull();
        assertThat(updated.getQuantity()).isEqualTo(150);
    }

    /**
     * Test deleting inventory.
     * Given: Existing inventory
     * When: delete is called
     * Then: Inventory is removed from database
     */
    @Test
    @DisplayName("Should delete inventory successfully")
    void shouldDeleteInventorySuccessfully() {
        // Given
        Inventory existing = inventoryRepository.findBySkuCodeIn(Collections.singletonList("iphone-13")).get(0);
        Long existingId = existing.getId();

        // When
        inventoryRepository.delete(existing);
        entityManager.flush();
        entityManager.clear();

        // Then
        boolean exists = inventoryRepository.findById(existingId).isPresent();
        assertThat(exists).isFalse();

        List<Inventory> remaining = inventoryRepository.findBySkuCodeIn(Collections.singletonList("iphone-13"));
        assertThat(remaining).isEmpty();
    }
}
