package com.seshrao.stockxpress.inventoryservice.service;

import com.seshrao.stockxpress.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class InventoryService {

    private final InventoryRepository inventoryRepository;

    @Transactional(readOnly = true) // method is marked as readOnly, meaning it will not participate in a transaction,
    // and therefore, any attempt to modify the database within this method will result in an exception.
    public boolean isInStock(String skuCode ){
        return inventoryRepository.findBySkuCode(skuCode).isPresent() ;
    }
}
