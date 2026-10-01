package com.seshrao.stockxpress.inventoryservice.service;

import com.seshrao.stockxpress.inventoryservice.dto.InventoryResponse;
import com.seshrao.stockxpress.inventoryservice.model.Inventory;
import com.seshrao.stockxpress.inventoryservice.repository.InventoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;

   // @SneakyThrows
    @Transactional(readOnly = true) // method is marked as readOnly, meaning it will not participate in a transaction,
    // and therefore, any attempt to modify the database within this method will result in an exception.
    public List<InventoryResponse> isInStock(List<String> skuCode ){

//        Testing the circuit breaker time-out
//        log.info("Wait Started");
//        Thread.sleep(10000);
//        log.info("Wait Ended");

        return inventoryRepository.findBySkuCodeIn(skuCode)
                .stream()
                .map(this::mapToDto).collect(Collectors.toList());
    }

    private InventoryResponse mapToDto(Inventory inventory) {
        return InventoryResponse.builder()
                .skuCode(inventory.getSkuCode())
                .isInStock(inventory.getQuantity() >0 )
                .build() ;
    }

}
