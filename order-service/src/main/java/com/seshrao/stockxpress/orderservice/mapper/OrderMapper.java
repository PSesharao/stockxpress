package com.seshrao.stockxpress.orderservice.mapper;

import com.seshrao.stockxpress.orderservice.dto.OrderLineItemDto;
import com.seshrao.stockxpress.orderservice.dto.OrderRequest;
import com.seshrao.stockxpress.orderservice.model.Order;
import com.seshrao.stockxpress.orderservice.model.OrderLineItem;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mapper component for converting between DTOs and Entities.
 * Centralizes all mapping logic following Single Responsibility Principle.
 */
@Component
@Slf4j
public class OrderMapper {

    /**
     * Maps OrderRequest DTO to Order entity.
     * Uses domain factory method to ensure business invariants are enforced.
     *
     * @param orderRequest The order request from the client
     * @return Order entity with generated order number
     */
    public Order toEntity(OrderRequest orderRequest) {
        log.debug("Mapping OrderRequest to Order entity");

        if (orderRequest == null) {
            log.warn("OrderRequest is null");
            return null;
        }

        List<OrderLineItem> orderLineItems = orderRequest.getOrderLineItemList()
                .stream()
                .map(this::toOrderLineItem)
                .collect(Collectors.toList());

        // Use domain factory method to create order with business validation
        return Order.createNewOrder(orderLineItems);
    }

    /**
     * Maps OrderLineItemDto to OrderLineItem entity.
     * Uses domain factory method to ensure business validation.
     *
     * @param dto The order line item DTO
     * @return OrderLineItem entity
     */
    public OrderLineItem toOrderLineItem(OrderLineItemDto dto) {
        if (dto == null) {
            log.warn("OrderLineItemDto is null");
            return null;
        }

        // Use domain factory method with validation
        return OrderLineItem.create(
                dto.getSkuCode(),
                dto.getPrice(),
                dto.getQuantity()
        );
    }
}
