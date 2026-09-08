package com.seshrao.stockxpress.orderservice.publisher;

import com.seshrao.stockxpress.orderservice.event.OrderPlacedEvent;
import com.seshrao.stockxpress.orderservice.model.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Kafka-based implementation of OrderEventPublisher.
 * Publishes order events to Kafka topics with proper error handling and logging.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KafkaOrderEventPublisher implements OrderEventPublisher {

    private static final String NOTIFICATION_TOPIC = "notificationTopic";
    private static final String EVENT_VERSION = "1.0";
    
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    @Override
    public void publishOrderPlaced(Order order) {
        log.info("Publishing order placed event for order number: {}", order.getOrderNumber());
        
        OrderPlacedEvent event = buildOrderPlacedEvent(order);
        
        ListenableFuture<SendResult<String, OrderPlacedEvent>> future = 
                kafkaTemplate.send(NOTIFICATION_TOPIC, order.getOrderNumber(), event);
        
        future.addCallback(new ListenableFutureCallback<SendResult<String, OrderPlacedEvent>>() {
            @Override
            public void onSuccess(SendResult<String, OrderPlacedEvent> result) {
                log.info("Successfully published order placed event. EventId: {}, OrderNumber: {}, Topic: {}, Partition: {}, Offset: {}",
                        event.getEventId(),
                        order.getOrderNumber(),
                        NOTIFICATION_TOPIC,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }

            @Override
            public void onFailure(Throwable ex) {
                log.error("Failed to publish order placed event for order number: {}. EventId: {}",
                        order.getOrderNumber(), event.getEventId(), ex);
                // In production, consider implementing retry logic or DLQ
            }
        });
    }

    /**
     * Builds a comprehensive OrderPlacedEvent from the Order entity.
     */
    private OrderPlacedEvent buildOrderPlacedEvent(Order order) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("source", "order-service");
        metadata.put("environment", "production");
        metadata.put("itemCount", order.getOrderLineItemList().size());
        
        return OrderPlacedEvent.builder()
                .eventId(UUID.randomUUID().toString())
                .eventVersion(EVENT_VERSION)
                .timestamp(LocalDateTime.now())
                .orderNumber(order.getOrderNumber())
                .orderId(order.getId())
                .orderLineItems(order.getOrderLineItemList().stream()
                        .map(item -> OrderPlacedEvent.OrderLineItemEvent.builder()
                                .skuCode(item.getSkuCode())
                                .price(item.getPrice())
                                .quantity(item.getQuantity())
                                .build())
                        .collect(Collectors.toList()))
                .metadata(metadata)
                .build();
    }
}
