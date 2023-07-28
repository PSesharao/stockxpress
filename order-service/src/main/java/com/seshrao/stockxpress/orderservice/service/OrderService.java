package com.seshrao.stockxpress.orderservice.service;

import com.seshrao.stockxpress.orderservice.dto.OrderLineItemDto;
import com.seshrao.stockxpress.orderservice.dto.OrderRequest;
import com.seshrao.stockxpress.orderservice.model.Order;
import com.seshrao.stockxpress.orderservice.model.OrderLineItem;
import com.seshrao.stockxpress.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional // to follow the ACID (Atomicity, Consistency, Isolation, Durability) properties to maintain data integrity.
//
public class OrderService {

    private final OrderRepository orderRepository;

    public void placeOrder(OrderRequest orderRequest){
        Order order = Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .orderLineItemList(orderRequest.getOrderLineItemList().stream()
                                .map(this::mapToDto).collect(Collectors.toList()))
                .build();

        orderRepository.save(order) ;
    }

    private OrderLineItem mapToDto(OrderLineItemDto orderLineItemDto) {
        OrderLineItem orderLineItem = OrderLineItem.builder()
                .skuCode(orderLineItemDto.getSkuCode())
                .price(orderLineItemDto.getPrice())
                .quantity(orderLineItemDto.getQuantity())
                .build();

        return orderLineItem ;
    }
}
