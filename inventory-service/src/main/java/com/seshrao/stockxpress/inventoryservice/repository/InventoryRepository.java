package com.seshrao.stockxpress.inventoryservice.repository;

import com.seshrao.stockxpress.inventoryservice.model.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryRepository extends JpaRepository<Inventory , Long> {

    // In Spring Data JPA, the findByFieldIn method allows you to find entities based on a collection of
    // values for a specific field. This method is used to retrieve records that match any of the given values
    // in the provided collection for a specified field.

    // List<Entity> findByFieldNameIn(Collection<FieldType> values);

    List<Inventory> findBySkuCodeIn(List<String> skuCode);
}
