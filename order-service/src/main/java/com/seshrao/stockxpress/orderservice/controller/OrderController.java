package com.seshrao.stockxpress.orderservice.controller;

import com.seshrao.stockxpress.orderservice.dto.OrderRequest;
import com.seshrao.stockxpress.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService ;

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public String placeOrder(@RequestBody OrderRequest orderRequest){
        orderService.placeOrder(orderRequest);
        return "Order placed successfully" ;
    }
}
