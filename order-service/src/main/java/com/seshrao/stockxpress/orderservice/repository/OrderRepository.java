package com.seshrao.stockxpress.orderservice.repository;

import com.seshrao.stockxpress.orderservice.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

}
