package com.seshrao.stockxpress.inventoryservice.controller;

import com.seshrao.stockxpress.inventoryservice.dto.InventoryResponse;
import com.seshrao.stockxpress.inventoryservice.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    // http://localhost:8082/api/inventory?skuCode=Iphone_13&skuCode=Iphone_13_red

    public List<InventoryResponse> isInStock(@RequestParam List<String> skuCode){
        return inventoryService.isInStock(skuCode) ;
    }

}
