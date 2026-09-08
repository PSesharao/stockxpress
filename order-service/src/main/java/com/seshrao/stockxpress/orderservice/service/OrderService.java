package com.seshrao.stockxpress.orderservice.service;

import com.seshrao.stockxpress.common.annotation.LogExecutionTime;
import com.seshrao.stockxpress.common.exception.BusinessException;
import com.seshrao.stockxpress.orderservice.client.InventoryClient;
import com.seshrao.stockxpress.orderservice.dto.InventoryResponse;
import com.seshrao.stockxpress.orderservice.dto.OrderRequest;
import com.seshrao.stockxpress.orderservice.mapper.OrderMapper;
import com.seshrao.stockxpress.orderservice.model.Order;
import com.seshrao.stockxpress.orderservice.model.OrderLineItem;
import com.seshrao.stockxpress.orderservice.publisher.OrderEventPublisher;
import com.seshrao.stockxpress.orderservice.repository.OrderRepository;
import com.seshrao.stockxpress.orderservice.validator.OrderValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Order Service - Refactored Phase 2
 * <p>
 * Responsibilities:
 * - Orchestrates order placement workflow
 * - Delegates validation to OrderValidator
 * - Uses OrderMapper for all DTO/Entity conversions
 * - Uses InventoryClient for inventory checks
 * - Uses OrderEventPublisher for event publishing
 * - Implements proper transaction management
 * - Implements compensating transactions for saga pattern
 * - Uses structured logging throughout
 * - Handles exceptions with custom exceptions from common-lib
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final InventoryClient inventoryClient;
    private final OrderEventPublisher orderEventPublisher;
    private final OrderMapper orderMapper;
    private final OrderValidator orderValidator;

    /**
     * Places an order with proper validation, inventory check, and event publishing.
     * Implements saga pattern with compensating transactions.
     *
     * @param orderRequest The order request from the client
     * @return Success message with order number
     * @throws com.seshrao.stockxpress.common.exception.ValidationException            if validation fails
     * @throws com.seshrao.stockxpress.common.exception.InsufficientInventoryException if items are out of stock
     * @throws com.seshrao.stockxpress.common.exception.ServiceUnavailableException    if inventory service is unavailable
     * @throws BusinessException                                                       if order placement fails
     */
    @LogExecutionTime(value = "placeOrder", logParameters = true, warnThresholdMillis = 3000)
    @Transactional(
            propagation = Propagation.REQUIRED,
            isolation = Isolation.READ_COMMITTED,
            rollbackFor = Exception.class
    )
    public String placeOrder(OrderRequest orderRequest) {
        log.info("Processing order placement request with {} items",
                orderRequest.getOrderLineItemList().size());

        Order savedOrder = null;
        boolean inventoryReserved = false;

        try {
            // Step 1: Validate order request
            log.debug("Step 1: Validating order request");
            orderValidator.validateOrderRequest(orderRequest);
            log.info("Order request validation successful");

            // Step 2: Map DTO to Entity
            log.debug("Step 2: Mapping order request to entity");
            Order order = orderMapper.toEntity(orderRequest);
            log.info("Order entity created with order number: {}", order.getOrderNumber());

            // Step 3: Check inventory availability
            log.debug("Step 3: Checking inventory availability");
            List<String> skuCodes = extractSkuCodes(order);
            log.info("Checking inventory for {} SKU codes: {}", skuCodes.size(), skuCodes);

            List<InventoryResponse> inventoryResponses = inventoryClient.checkInventory(skuCodes);
            log.info("Inventory check completed. Received {} responses", inventoryResponses.size());

            // Step 4: Validate inventory availability
            log.debug("Step 4: Validating inventory availability");
            orderValidator.validateInventoryAvailability(inventoryResponses);
            log.info("All items are in stock. Proceeding with order placement");

            inventoryReserved = true;

            // Step 5: Save order to database
            log.debug("Step 5: Persisting order to database");
            savedOrder = orderRepository.save(order);
            log.info("Order saved successfully with ID: {} and order number: {}",
                    savedOrder.getId(), savedOrder.getOrderNumber());

            // Step 6: Publish order placed event
            log.debug("Step 6: Publishing order placed event");
            orderEventPublisher.publishOrderPlaced(savedOrder);
            log.info("Order placed event published for order number: {}",
                    savedOrder.getOrderNumber());

            String successMessage = String.format(
                    "Order placed successfully. Order Number: %s",
                    savedOrder.getOrderNumber()
            );
            log.info("Order placement completed successfully: {}", successMessage);

            return successMessage;

        } catch (Exception e) {
            log.error("Error occurred during order placement. Initiating compensating transaction", e);

            // Compensating Transaction - Saga Pattern
            executeCompensatingTransaction(savedOrder, inventoryReserved);

            // Re-throw the exception after compensation
            throw e;
        }
    }

    /**
     * Executes compensating transaction in case of failure.
     * Part of Saga pattern implementation.
     *
     * @param savedOrder        The order that was saved (null if not saved)
     * @param inventoryReserved Whether inventory was reserved
     */
    private void executeCompensatingTransaction(Order savedOrder, boolean inventoryReserved) {
        log.warn("Executing compensating transaction for failed order placement");

        try {
            // Rollback: Delete saved order if it exists
            if (savedOrder != null && savedOrder.getId() != null) {
                log.info("Rolling back: Deleting order with ID: {}", savedOrder.getId());
                orderRepository.deleteById(savedOrder.getId());
                log.info("Order successfully deleted as part of compensation");
            }

            // Note: In a real saga pattern, we would also need to:
            // 1. Release reserved inventory (if inventory service supports reservation)
            // 2. Publish a compensation event to notify other services
            // 3. Update order status to CANCELLED/FAILED

            if (inventoryReserved) {
                log.info("Inventory was reserved. In production, would trigger inventory release");
                // TODO: Implement inventory release API call when available
                // inventoryClient.releaseInventory(skuCodes);
            }

            log.info("Compensating transaction completed successfully");

        } catch (Exception compensationError) {
            // Log but don't throw - we don't want to mask the original error
            log.error("Error during compensating transaction execution. Manual intervention may be required",
                    compensationError);

            // In production, this should trigger an alert for manual intervention
            // alertingService.sendCriticalAlert("Compensating transaction failed", compensationError);
        }
    }

    /**
     * Extracts SKU codes from order line items.
     *
     * @param order The order entity
     * @return List of SKU codes
     */
    private List<String> extractSkuCodes(Order order) {
        return order.getOrderLineItemList()
                .stream()
                .map(OrderLineItem::getSkuCode)
                .collect(Collectors.toList());
    }
}
