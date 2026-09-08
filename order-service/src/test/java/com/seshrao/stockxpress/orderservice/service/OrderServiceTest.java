package com.seshrao.stockxpress.orderservice.service;

import com.seshrao.stockxpress.orderservice.dto.InventoryResponse;
import com.seshrao.stockxpress.orderservice.dto.OrderLineItemDto;
import com.seshrao.stockxpress.orderservice.dto.OrderRequest;
import com.seshrao.stockxpress.orderservice.event.OrderPlacedEvent;
import com.seshrao.stockxpress.orderservice.model.Order;
import com.seshrao.stockxpress.orderservice.model.OrderLineItem;
import com.seshrao.stockxpress.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.sleuth.Span;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for OrderService.
 * Tests order placement with WebClient, KafkaTemplate, and Tracer mocking.
 * Covers success scenarios, stock validation, Kafka messaging, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private Tracer tracer;

    @Mock
    private Span span;

    @Mock
    private Tracer.SpanInScope spanInScope;

    @Mock
    private KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @InjectMocks
    private OrderService orderService;

    private OrderRequest orderRequest;
    private OrderLineItemDto orderLineItemDto1;
    private OrderLineItemDto orderLineItemDto2;
    private InventoryResponse[] inventoryResponsesInStock;
    private InventoryResponse[] inventoryResponsesOutOfStock;

    @BeforeEach
    void setUp() {
        // Given - Setup test data
        orderLineItemDto1 = new OrderLineItemDto();
        orderLineItemDto1.setSkuCode("iphone-13");
        orderLineItemDto1.setPrice(new BigDecimal("999.99"));
        orderLineItemDto1.setQuantity(2);

        orderLineItemDto2 = new OrderLineItemDto();
        orderLineItemDto2.setSkuCode("macbook-pro");
        orderLineItemDto2.setPrice(new BigDecimal("2499.99"));
        orderLineItemDto2.setQuantity(1);

        orderRequest = new OrderRequest();
        orderRequest.setOrderLineItemList(Arrays.asList(orderLineItemDto1, orderLineItemDto2));

        // Setup inventory responses
        InventoryResponse inventoryResponse1 = new InventoryResponse();
        inventoryResponse1.setSkuCode("iphone-13");
        inventoryResponse1.setInStock(true);

        InventoryResponse inventoryResponse2 = new InventoryResponse();
        inventoryResponse2.setSkuCode("macbook-pro");
        inventoryResponse2.setInStock(true);

        inventoryResponsesInStock = new InventoryResponse[]{inventoryResponse1, inventoryResponse2};

        // Out of stock scenario
        InventoryResponse inventoryResponseOutOfStock = new InventoryResponse();
        inventoryResponseOutOfStock.setSkuCode("iphone-13");
        inventoryResponseOutOfStock.setInStock(false);

        inventoryResponsesOutOfStock = new InventoryResponse[]{inventoryResponseOutOfStock, inventoryResponse2};

        // Setup default mock behavior for Tracer and Span
        when(tracer.nextSpan()).thenReturn(span);
        when(span.name(anyString())).thenReturn(span);
        when(span.start()).thenReturn(span);
        when(tracer.withSpan(any(Span.class))).thenReturn(spanInScope);
        doNothing().when(span).end();
    }

    /**
     * Test placing order successfully when all products are in stock.
     * Given: Valid order request with items in stock
     * When: placeOrder is called
     * Then: Order is saved, Kafka message is sent, and success message is returned
     */
    @Test
    @DisplayName("Should place order successfully when all products are in stock")
    void shouldPlaceOrderSuccessfullyWhenAllProductsInStock() {
        // Given
        setupWebClientMocks(inventoryResponsesInStock);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(anyString(), any(OrderPlacedEvent.class))).thenReturn(null);

        // When
        String result = orderService.placeOrder(orderRequest);

        // Then
        assertThat(result).isEqualTo("Order Placed Successfully");
        
        // Verify order repository was called
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        Order savedOrder = orderCaptor.getValue();
        assertThat(savedOrder.getOrderNumber()).isNotNull();
        assertThat(savedOrder.getOrderLineItemList()).hasSize(2);
        assertThat(savedOrder.getOrderLineItemList().get(0).getSkuCode()).isEqualTo("iphone-13");
        assertThat(savedOrder.getOrderLineItemList().get(1).getSkuCode()).isEqualTo("macbook-pro");

        // Verify Kafka message was sent
        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        verify(kafkaTemplate, times(1)).send(eq("notificationTopic"), eventCaptor.capture());
        OrderPlacedEvent sentEvent = eventCaptor.getValue();
        assertThat(sentEvent.getOrderNumber()).isEqualTo(savedOrder.getOrderNumber());

        // Verify span was created and closed
        verify(tracer, times(1)).nextSpan();
        verify(span, times(1)).name("InventoryServiceLookup");
        verify(span, times(1)).start();
        verify(span, times(1)).end();
    }

    /**
     * Test placing order fails when product is out of stock.
     * Given: Order request with out-of-stock items
     * When: placeOrder is called
     * Then: IllegalArgumentException is thrown and order is not saved
     */
    @Test
    @DisplayName("Should throw exception when product is out of stock")
    void shouldThrowExceptionWhenProductOutOfStock() {
        // Given
        setupWebClientMocks(inventoryResponsesOutOfStock);

        // When & Then
        assertThatThrownBy(() -> orderService.placeOrder(orderRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product is not in stock , please try again later");

        // Verify order was not saved
        verify(orderRepository, never()).save(any(Order.class));
        
        // Verify Kafka message was not sent
        verify(kafkaTemplate, never()).send(anyString(), any(OrderPlacedEvent.class));

        // Verify span was still closed even after exception
        verify(span, times(1)).end();
    }

    /**
     * Test placing order with single line item.
     * Given: Order request with single item in stock
     * When: placeOrder is called
     * Then: Order is placed successfully
     */
    @Test
    @DisplayName("Should place order successfully with single line item")
    void shouldPlaceOrderWithSingleLineItem() {
        // Given
        orderRequest.setOrderLineItemList(Collections.singletonList(orderLineItemDto1));
        InventoryResponse singleInventoryResponse = new InventoryResponse();
        singleInventoryResponse.setSkuCode("iphone-13");
        singleInventoryResponse.setInStock(true);
        setupWebClientMocks(new InventoryResponse[]{singleInventoryResponse});
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(anyString(), any(OrderPlacedEvent.class))).thenReturn(null);

        // When
        String result = orderService.placeOrder(orderRequest);

        // Then
        assertThat(result).isEqualTo("Order Placed Successfully");
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getOrderLineItemList()).hasSize(1);
        verify(kafkaTemplate, times(1)).send(anyString(), any(OrderPlacedEvent.class));
    }

    /**
     * Test placing order with empty inventory response.
     * Given: Order request but inventory service returns empty array
     * When: placeOrder is called
     * Then: Order is placed successfully (edge case - empty array passes allMatch)
     */
    @Test
    @DisplayName("Should handle empty inventory response")
    void shouldHandleEmptyInventoryResponse() {
        // Given
        setupWebClientMocks(new InventoryResponse[]{});
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(anyString(), any(OrderPlacedEvent.class))).thenReturn(null);

        // When
        String result = orderService.placeOrder(orderRequest);

        // Then - Empty array with allMatch returns true (edge case)
        assertThat(result).isEqualTo("Order Placed Successfully");
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    /**
     * Test placing order with multiple line items having same SKU.
     * Given: Order request with duplicate SKU codes
     * When: placeOrder is called
     * Then: Order is processed correctly
     */
    @Test
    @DisplayName("Should handle order with duplicate SKU codes")
    void shouldHandleOrderWithDuplicateSkuCodes() {
        // Given
        OrderLineItemDto duplicateItem = new OrderLineItemDto();
        duplicateItem.setSkuCode("iphone-13");
        duplicateItem.setPrice(new BigDecimal("999.99"));
        duplicateItem.setQuantity(3);
        orderRequest.setOrderLineItemList(Arrays.asList(orderLineItemDto1, duplicateItem));
        
        setupWebClientMocks(inventoryResponsesInStock);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(anyString(), any(OrderPlacedEvent.class))).thenReturn(null);

        // When
        String result = orderService.placeOrder(orderRequest);

        // Then
        assertThat(result).isEqualTo("Order Placed Successfully");
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    /**
     * Test placing order with zero quantity.
     * Given: Order request with zero quantity items
     * When: placeOrder is called
     * Then: Order is processed (business validation should happen elsewhere)
     */
    @Test
    @DisplayName("Should process order with zero quantity items")
    void shouldProcessOrderWithZeroQuantity() {
        // Given
        orderLineItemDto1.setQuantity(0);
        setupWebClientMocks(inventoryResponsesInStock);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(anyString(), any(OrderPlacedEvent.class))).thenReturn(null);

        // When
        String result = orderService.placeOrder(orderRequest);

        // Then
        assertThat(result).isEqualTo("Order Placed Successfully");
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    /**
     * Test placing order with negative price.
     * Given: Order request with negative price items
     * When: placeOrder is called
     * Then: Order is processed (business validation should happen elsewhere)
     */
    @Test
    @DisplayName("Should process order with negative price")
    void shouldProcessOrderWithNegativePrice() {
        // Given
        orderLineItemDto1.setPrice(new BigDecimal("-99.99"));
        setupWebClientMocks(inventoryResponsesInStock);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(anyString(), any(OrderPlacedEvent.class))).thenReturn(null);

        // When
        String result = orderService.placeOrder(orderRequest);

        // Then
        assertThat(result).isEqualTo("Order Placed Successfully");
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    /**
     * Test placing order with null SKU code.
     * Given: Order request with null SKU code
     * When: placeOrder is called
     * Then: Order is processed (null will be included in query params)
     */
    @Test
    @DisplayName("Should handle order with null SKU code")
    void shouldHandleOrderWithNullSkuCode() {
        // Given
        orderLineItemDto1.setSkuCode(null);
        setupWebClientMocks(inventoryResponsesInStock);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(anyString(), any(OrderPlacedEvent.class))).thenReturn(null);

        // When
        String result = orderService.placeOrder(orderRequest);

        // Then
        assertThat(result).isEqualTo("Order Placed Successfully");
        verify(orderRepository, times(1)).save(any(Order.class));
    }

    /**
     * Test that span is closed even when exception occurs.
     * Given: Order request that will cause exception
     * When: placeOrder is called and exception occurs
     * Then: Span is properly closed in finally block
     */
    @Test
    @DisplayName("Should close span in finally block even on exception")
    void shouldCloseSpanInFinallyBlockOnException() {
        // Given
        setupWebClientMocks(inventoryResponsesOutOfStock);

        // When
        try {
            orderService.placeOrder(orderRequest);
        } catch (IllegalArgumentException e) {
            // Expected exception
        }

        // Then
        verify(span, times(1)).end();
    }

    /**
     * Test WebClient is called with correct URI and parameters.
     * Given: Valid order request
     * When: placeOrder is called
     * Then: WebClient is invoked with proper inventory service URI
     */
    @Test
    @DisplayName("Should call WebClient with correct inventory service URI")
    void shouldCallWebClientWithCorrectUri() {
        // Given
        setupWebClientMocks(inventoryResponsesInStock);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(anyString(), any(OrderPlacedEvent.class))).thenReturn(null);

        // When
        orderService.placeOrder(orderRequest);

        // Then
        verify(webClientBuilder, times(1)).build();
        verify(webClient, times(1)).get();
        verify(requestHeadersUriSpec, times(1)).uri(anyString(), any(Function.class));
    }

    /**
     * Test Kafka message contains correct order number.
     * Given: Successful order placement
     * When: placeOrder is called
     * Then: Kafka message has matching order number from saved order
     */
    @Test
    @DisplayName("Should send Kafka message with correct order number")
    void shouldSendKafkaMessageWithCorrectOrderNumber() {
        // Given
        setupWebClientMocks(inventoryResponsesInStock);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(anyString(), any(OrderPlacedEvent.class))).thenReturn(null);

        // When
        orderService.placeOrder(orderRequest);

        // Then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        ArgumentCaptor<OrderPlacedEvent> eventCaptor = ArgumentCaptor.forClass(OrderPlacedEvent.class);
        
        verify(orderRepository).save(orderCaptor.capture());
        verify(kafkaTemplate).send(eq("notificationTopic"), eventCaptor.capture());
        
        assertThat(eventCaptor.getValue().getOrderNumber())
                .isEqualTo(orderCaptor.getValue().getOrderNumber());
    }

    /**
     * Test order number is generated as UUID.
     * Given: Valid order request
     * When: placeOrder is called
     * Then: Order number is a valid UUID string
     */
    @Test
    @DisplayName("Should generate UUID for order number")
    void shouldGenerateUuidForOrderNumber() {
        // Given
        setupWebClientMocks(inventoryResponsesInStock);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(anyString(), any(OrderPlacedEvent.class))).thenReturn(null);

        // When
        orderService.placeOrder(orderRequest);

        // Then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        String orderNumber = orderCaptor.getValue().getOrderNumber();
        
        // UUID format: 8-4-4-4-12 hexadecimal characters
        assertThat(orderNumber).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    /**
     * Test order line items are correctly mapped from DTO.
     * Given: Order request with multiple line items
     * When: placeOrder is called
     * Then: OrderLineItem entities have correct values from DTOs
     */
    @Test
    @DisplayName("Should correctly map OrderLineItemDto to OrderLineItem entities")
    void shouldCorrectlyMapDtoToEntity() {
        // Given
        setupWebClientMocks(inventoryResponsesInStock);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kafkaTemplate.send(anyString(), any(OrderPlacedEvent.class))).thenReturn(null);

        // When
        orderService.placeOrder(orderRequest);

        // Then
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        List<OrderLineItem> items = orderCaptor.getValue().getOrderLineItemList();
        
        assertThat(items).hasSize(2);
        assertThat(items.get(0).getSkuCode()).isEqualTo("iphone-13");
        assertThat(items.get(0).getPrice()).isEqualByComparingTo(new BigDecimal("999.99"));
        assertThat(items.get(0).getQuantity()).isEqualTo(2);
        assertThat(items.get(1).getSkuCode()).isEqualTo("macbook-pro");
        assertThat(items.get(1).getPrice()).isEqualByComparingTo(new BigDecimal("2499.99"));
        assertThat(items.get(1).getQuantity()).isEqualTo(1);
    }

    /**
     * Test partial stock availability scenario.
     * Given: Order with mixed stock availability (some in stock, some out)
     * When: placeOrder is called
     * Then: Exception is thrown as not all items are in stock
     */
    @Test
    @DisplayName("Should throw exception when only some items are in stock")
    void shouldThrowExceptionWhenPartialStockAvailability() {
        // Given - Mixed stock scenario
        InventoryResponse response1 = new InventoryResponse();
        response1.setSkuCode("iphone-13");
        response1.setInStock(true);
        
        InventoryResponse response2 = new InventoryResponse();
        response2.setSkuCode("macbook-pro");
        response2.setInStock(false);
        
        setupWebClientMocks(new InventoryResponse[]{response1, response2});

        // When & Then
        assertThatThrownBy(() -> orderService.placeOrder(orderRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product is not in stock , please try again later");
        
        verify(orderRepository, never()).save(any(Order.class));
        verify(kafkaTemplate, never()).send(anyString(), any(OrderPlacedEvent.class));
    }

    /**
     * Helper method to setup WebClient mocks for inventory service call.
     * 
     * @param inventoryResponses the array of inventory responses to return
     */
    private void setupWebClientMocks(InventoryResponse[] inventoryResponses) {
        when(webClientBuilder.build()).thenReturn(webClient);
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString(), any(Function.class))).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(InventoryResponse[].class)).thenReturn(Mono.just(inventoryResponses));
    }
}
