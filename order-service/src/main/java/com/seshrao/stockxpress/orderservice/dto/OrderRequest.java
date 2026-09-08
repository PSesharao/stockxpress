package com.seshrao.stockxpress.orderservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * DTO for order placement requests.
 * Contains validation annotations for automatic validation.
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {

    @NotNull(message = "Order line item list cannot be null")
    @NotEmpty(message = "Order must contain at least one item")
    @Valid
    private List<OrderLineItemDto> orderLineItemList;
}
