package com.seshrao.stockxpress.orderservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;

/**
 * Main application class for Order Service.
 * Enables caching, Eureka client for service discovery, and component scanning for common-lib.
 *
 * @author StockXpress Team
 */
@SpringBootApplication
@EnableEurekaClient
@EnableCaching
@ComponentScan(basePackages = {
        "com.seshrao.stockxpress.orderservice",
        "com.seshrao.stockxpress.common"
})
public class OrderServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }

}
