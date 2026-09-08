package com.seshrao.stockxpress.notificationservice;

import com.seshrao.stockxpress.notificationservice.event.OrderPlacedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.EnableEurekaClient;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.kafka.annotation.KafkaListener;

/**
 * Main application class for Notification Service.
 * Handles order notification events via Kafka and component scanning for common-lib.
 *
 * @author StockXpress Team
 */
@EnableEurekaClient
@SpringBootApplication
@ComponentScan(basePackages = {
        "com.seshrao.stockxpress.notificationservice",
        "com.seshrao.stockxpress.common"
})
@Slf4j
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

    @KafkaListener(topics = "notificationTopic")
    public void handleNotification(OrderPlacedEvent orderPlacedEvent) {
        // send out email notification
        log.info("Received Notification for Order - {}", orderPlacedEvent.getOrderNumber());
    }
}
