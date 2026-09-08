package com.seshrao.stockxpress.productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;

/**
 * Product Service Application - Manages product catalog for StockXpress.
 * <p>
 * Features:
 * - Product CRUD operations
 * - MongoDB for product storage
 * - Caching for performance optimization
 * - Integration with common-lib for shared functionality
 * - Distributed tracing with Sleuth and Zipkin
 *
 * @author StockXpress Team
 */
@SpringBootApplication(scanBasePackages = {
        "com.seshrao.stockxpress.productservice",
        "com.seshrao.stockxpress.common"
})
@EnableEurekaClient
@EnableCaching
public class ProductServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

}
