package com.seshrao.stockxpress.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderLineItemDto {

    private String skuCode ;

    private BigDecimal price ;

    private Integer quantity ;

}
