package com.seshrao.stockxpress.orderservice.validator;

import com.seshrao.stockxpress.common.exception.InsufficientInventoryException;
import com.seshrao.stockxpress.common.exception.ValidationException;
import com.seshrao.stockxpress.orderservice.dto.InventoryResponse;
import com.seshrao.stockxpress.orderservice.dto.OrderLineItemDto;
import com.seshrao.stockxpress.orderservice.dto.OrderRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Validator component for order-related business validations.
 * Centralizes all validation logic following Single Responsibility Principle.
 */
@Component
@Slf4j
public class OrderValidator {

    /**
     * Validates the order request for business rules.
     *
     * @param orderRequest The order request to validate
     * @throws ValidationException if validation fails
     */
    public void validateOrderRequest(OrderRequest orderRequest) {
        log.debug("Validating order request");
        
        if (orderRequest == null) {
            throw new ValidationException("Order request cannot be null");
        }
        
        if (CollectionUtils.isEmpty(orderRequest.getOrderLineItemList())) {
            throw new ValidationException("Order must contain at least one item");
        }
        
        validateOrderLineItems(orderRequest.getOrderLineItemList());
    }

    /**
     * Validates individual order line items.
     *
     * @param orderLineItems List of order line items to validate
     * @throws ValidationException if any item is invalid
     */
    private void validateOrderLineItems(List<OrderLineItemDto> orderLineItems) {
        for (int i = 0; i < orderLineItems.size(); i++) {
            OrderLineItemDto item = orderLineItems.get(i);
            
            if (item == null) {
                throw new ValidationException("Order line item at index " + i + " is null");
            }
            
            if (item.getSkuCode() == null || item.getSkuCode().trim().isEmpty()) {
                throw new ValidationException("SKU code is required for item at index " + i);
            }
            
            if (item.getQuantity() == null || item.getQuantity() <= 0) {
                throw new ValidationException(
                    "Quantity must be greater than 0 for SKU: " + item.getSkuCode());
            }
            
            if (item.getPrice() == null || item.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw new ValidationException(
                    "Price must be greater than 0 for SKU: " + item.getSkuCode());
            }
        }
    }

    /**
     * Validates inventory availability for all items.
     *
     * @param inventoryResponses List of inventory responses from inventory service
     * @throws InsufficientInventoryException if any item is out of stock
     */
    public void validateInventoryAvailability(List<InventoryResponse> inventoryResponses) {
        log.debug("Validating inventory availability for {} items", inventoryResponses.size());
        
        if (CollectionUtils.isEmpty(inventoryResponses)) {
            throw new InsufficientInventoryException(
                "Unable to verify inventory availability. Please try again later.");
        }
        
        List<String> outOfStockItems = inventoryResponses.stream()
                .filter(response -> !response.isInStock())
                .map(InventoryResponse::getSkuCode)
                .collect(Collectors.toList());
        
        if (!outOfStockItems.isEmpty()) {
            String skuCodes = String.join(", ", outOfStockItems);
            log.warn("Items out of stock: {}", skuCodes);
            throw new InsufficientInventoryException(
                "The following items are out of stock: " + skuCodes + ". Please try again later.");
        }
        
        log.info("All items are in stock");
    }
}
