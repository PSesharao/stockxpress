package com.seshrao.stockxpress.orderservice.service;

import com.seshrao.stockxpress.orderservice.dto.InventoryResponse;
import com.seshrao.stockxpress.orderservice.dto.OrderLineItemDto;
import com.seshrao.stockxpress.orderservice.dto.OrderRequest;
import com.seshrao.stockxpress.orderservice.model.Order;
import com.seshrao.stockxpress.orderservice.model.OrderLineItem;
import com.seshrao.stockxpress.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional // to follow the ACID (Atomicity, Consistency, Isolation, Durability) properties to maintain data integrity.
//
public class OrderService {

    private final OrderRepository orderRepository;
    private final WebClient webClient ;

    public void placeOrder(OrderRequest orderRequest){
        Order order = Order.builder()
                .orderNumber(UUID.randomUUID().toString())
                .orderLineItemList(orderRequest.getOrderLineItemList().stream()
                                .map(this::mapToDto).collect(Collectors.toList()))
                .build();
        // Call Inventory service, and place order if the product is in stock

        List<String> skuCodeList = order.getOrderLineItemList()
                .stream()
                .map(OrderLineItem::getSkuCode)
                .collect(Collectors.toList());

        InventoryResponse[] inventoryResponseArray = webClient.get()
               // .uri("http://localhost:8082/api/inventory",
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode",skuCodeList).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block();// Synchronous request

        boolean allProductsinStock = Arrays.stream(inventoryResponseArray).allMatch(InventoryResponse::isInStock);
        // If all the items is in stock , then that expression becomes true

        if(allProductsinStock) {
            orderRepository.save(order);
        }else{
            throw new IllegalArgumentException("Product is not in stock , please try again later") ;
        }

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
