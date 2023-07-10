package com.seshrao.stockxpress.productservice.repository;

import com.seshrao.stockxpress.productservice.model.Product;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends MongoRepository<Product , String> {
}
