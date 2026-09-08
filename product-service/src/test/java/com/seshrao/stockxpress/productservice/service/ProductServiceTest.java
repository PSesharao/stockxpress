package com.seshrao.stockxpress.productservice.service;

import com.seshrao.stockxpress.productservice.dto.ProductRequest;
import com.seshrao.stockxpress.productservice.dto.ProductResponse;
import com.seshrao.stockxpress.productservice.model.Product;
import com.seshrao.stockxpress.productservice.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ProductService.
 * Tests business logic for product creation and retrieval.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private ProductRequest productRequest;
    private Product product;

    @BeforeEach
    void setUp() {
        // Given - Setup test data
        productRequest = ProductRequest.builder()
                .name("iPhone 13")
                .description("Latest iPhone model")
                .price(new BigDecimal("999.99"))
                .build();

        product = Product.builder()
                .id("507f1f77bcf86cd799439011")
                .name("iPhone 13")
                .description("Latest iPhone model")
                .price(new BigDecimal("999.99"))
                .build();
    }

    /**
     * Test creating a product successfully.
     * Given: Valid product request
     * When: createProduct is called
     * Then: Product is saved to repository with correct attributes
     */
    @Test
    @DisplayName("Should create product successfully and save to repository")
    void shouldCreateProductSuccessfully() {
        // Given
        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        productService.createProduct(productRequest);

        // Then
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct).isNotNull();
        assertThat(savedProduct.getName()).isEqualTo("iPhone 13");
        assertThat(savedProduct.getDescription()).isEqualTo("Latest iPhone model");
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
    }

    /**
     * Test creating a product with null name.
     * Given: Product request with null name
     * When: createProduct is called
     * Then: Product with null name is saved to repository
     */
    @Test
    @DisplayName("Should create product with null name")
    void shouldCreateProductWithNullName() {
        // Given
        ProductRequest requestWithNullName = ProductRequest.builder()
                .name(null)
                .description("Test description")
                .price(new BigDecimal("100.00"))
                .build();

        Product productWithNullName = Product.builder()
                .name(null)
                .description("Test description")
                .price(new BigDecimal("100.00"))
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(productWithNullName);

        // When
        productService.createProduct(requestWithNullName);

        // Then
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getName()).isNull();
    }

    /**
     * Test creating a product with null description.
     * Given: Product request with null description
     * When: createProduct is called
     * Then: Product with null description is saved to repository
     */
    @Test
    @DisplayName("Should create product with null description")
    void shouldCreateProductWithNullDescription() {
        // Given
        ProductRequest requestWithNullDescription = ProductRequest.builder()
                .name("Test Product")
                .description(null)
                .price(new BigDecimal("100.00"))
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        productService.createProduct(requestWithNullDescription);

        // Then
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getDescription()).isNull();
    }

    /**
     * Test creating a product with null price.
     * Given: Product request with null price
     * When: createProduct is called
     * Then: Product with null price is saved to repository
     */
    @Test
    @DisplayName("Should create product with null price")
    void shouldCreateProductWithNullPrice() {
        // Given
        ProductRequest requestWithNullPrice = ProductRequest.builder()
                .name("Test Product")
                .description("Test description")
                .price(null)
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        productService.createProduct(requestWithNullPrice);

        // Then
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getPrice()).isNull();
    }

    /**
     * Test creating a product with zero price.
     * Given: Product request with zero price
     * When: createProduct is called
     * Then: Product with zero price is saved to repository
     */
    @Test
    @DisplayName("Should create product with zero price")
    void shouldCreateProductWithZeroPrice() {
        // Given
        ProductRequest requestWithZeroPrice = ProductRequest.builder()
                .name("Free Product")
                .description("Free item")
                .price(BigDecimal.ZERO)
                .build();

        when(productRepository.save(any(Product.class))).thenReturn(product);

        // When
        productService.createProduct(requestWithZeroPrice);

        // Then
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository, times(1)).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();
        assertThat(savedProduct.getPrice()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    /**
     * Test retrieving all products successfully.
     * Given: Multiple products exist in repository
     * When: getAllProducts is called
     * Then: All products are returned as ProductResponse list
     */
    @Test
    @DisplayName("Should retrieve all products successfully")
    void shouldGetAllProductsSuccessfully() {
        // Given
        Product product2 = Product.builder()
                .id("507f1f77bcf86cd799439012")
                .name("MacBook Pro")
                .description("Latest MacBook model")
                .price(new BigDecimal("2499.99"))
                .build();

        List<Product> products = Arrays.asList(product, product2);
        when(productRepository.findAll()).thenReturn(products);

        // When
        List<ProductResponse> result = productService.getAllProducts();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo("507f1f77bcf86cd799439011");
        assertThat(result.get(0).getName()).isEqualTo("iPhone 13");
        assertThat(result.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(result.get(1).getId()).isEqualTo("507f1f77bcf86cd799439012");
        assertThat(result.get(1).getName()).isEqualTo("MacBook Pro");
        assertThat(result.get(1).getPrice()).isEqualByComparingTo(new BigDecimal("2499.99"));

        verify(productRepository, times(1)).findAll();
    }

    /**
     * Test retrieving all products when repository is empty.
     * Given: No products exist in repository
     * When: getAllProducts is called
     * Then: Empty list is returned
     */
    @Test
    @DisplayName("Should return empty list when no products exist")
    void shouldReturnEmptyListWhenNoProductsExist() {
        // Given
        when(productRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<ProductResponse> result = productService.getAllProducts();

        // Then
        assertThat(result).isNotNull();
        assertThat(result).isEmpty();
        verify(productRepository, times(1)).findAll();
    }

    /**
     * Test mapping product to product response.
     * Given: Product with all fields populated
     * When: Product is mapped to ProductResponse
     * Then: All fields are correctly mapped
     */
    @Test
    @DisplayName("Should map product to product response correctly")
    void shouldMapProductToProductResponse() {
        // Given
        when(productRepository.findAll()).thenReturn(Collections.singletonList(product));

        // When
        List<ProductResponse> result = productService.getAllProducts();

        // Then
        assertThat(result).hasSize(1);
        ProductResponse response = result.get(0);
        assertThat(response.getId()).isEqualTo(product.getId());
        assertThat(response.getName()).isEqualTo(product.getName());
        assertThat(response.getDescription()).isEqualTo(product.getDescription());
        assertThat(response.getPrice()).isEqualByComparingTo(product.getPrice());
    }

    /**
     * Test repository exception handling during product creation.
     * Given: Repository throws exception on save
     * When: createProduct is called
     * Then: Exception is propagated
     */
    @Test
    @DisplayName("Should propagate exception when repository fails during save")
    void shouldPropagateExceptionWhenRepositoryFailsDuringSave() {
        // Given
        when(productRepository.save(any(Product.class)))
                .thenThrow(new RuntimeException("MongoDB connection failed"));

        // When & Then
        try {
            productService.createProduct(productRequest);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("MongoDB connection failed");
        }

        verify(productRepository, times(1)).save(any(Product.class));
    }

    /**
     * Test repository exception handling during product retrieval.
     * Given: Repository throws exception on findAll
     * When: getAllProducts is called
     * Then: Exception is propagated
     */
    @Test
    @DisplayName("Should propagate exception when repository fails during findAll")
    void shouldPropagateExceptionWhenRepositoryFailsDuringFindAll() {
        // Given
        when(productRepository.findAll())
                .thenThrow(new RuntimeException("MongoDB connection failed"));

        // When & Then
        try {
            productService.getAllProducts();
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).isEqualTo("MongoDB connection failed");
        }

        verify(productRepository, times(1)).findAll();
    }
}
