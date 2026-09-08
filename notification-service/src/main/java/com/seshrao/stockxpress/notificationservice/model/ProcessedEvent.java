package com.seshrao.stockxpress.notificationservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to track processed events for idempotent consumer pattern.
 * Prevents duplicate event processing by maintaining a record of all processed event IDs.
 */
@Entity
@Table(name = "processed_events", indexes = {
    @Index(name = "idx_event_id", columnList = "event_id", unique = true),
    @Index(name = "idx_processed_at", columnList = "processed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 100)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "order_number", length = 100)
    private String orderNumber;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;

    @Column(name = "notification_types", length = 500)
    private String notificationTypes;  // Comma-separated list of notification types sent

    @Column(name = "status", length = 20)
    private String status;  // SUCCESS, FAILED, PARTIAL

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (processedAt == null) {
            processedAt = LocalDateTime.now();
        }
    }
}