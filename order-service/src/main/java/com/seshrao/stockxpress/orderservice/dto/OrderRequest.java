package com.seshrao.stockxpress.orderservice.dto;

import com.seshrao.stockxpress.orderservice.model.OrderLineItem;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    private List<OrderLineItemDto> orderLineItemList ;
}
