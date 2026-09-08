package com.seshrao.stockxpress.notificationservice.repository;

import com.seshrao.stockxpress.notificationservice.model.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ProcessedEvent entity.
 * Provides data access operations for idempotent event processing.
 */
@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {

    /**
     * Check if an event ID has already been processed.
     *
     * @param eventId the event ID to check
     * @return true if event exists, false otherwise
     */
    boolean existsByEventId(String eventId);

    /**
     * Find a processed event by event ID.
     *
     * @param eventId the event ID
     * @return Optional containing the processed event if found
     */
    Optional<ProcessedEvent> findByEventId(String eventId);

    /**
     * Find all processed events by event type.
     *
     * @param eventType the event type
     * @return list of processed events
     */
    List<ProcessedEvent> findByEventType(String eventType);

    /**
     * Find all processed events by status.
     *
     * @param status the processing status
     * @return list of processed events
     */
    List<ProcessedEvent> findByStatus(String status);

    /**
     * Find all failed events for retry processing.
     *
     * @return list of failed processed events
     */
    @Query("SELECT pe FROM ProcessedEvent pe WHERE pe.status = 'FAILED' ORDER BY pe.processedAt DESC")
    List<ProcessedEvent> findFailedEvents();

    /**
     * Find processed events within a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of processed events
     */
    @Query("SELECT pe FROM ProcessedEvent pe WHERE pe.processedAt BETWEEN :startDate AND :endDate ORDER BY pe.processedAt DESC")
    List<ProcessedEvent> findByProcessedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                                    @Param("endDate") LocalDateTime endDate);

    /**
     * Delete old processed events (for cleanup/archiving).
     *
     * @param cutoffDate the cutoff date
     * @return number of deleted records
     */
    @Query("DELETE FROM ProcessedEvent pe WHERE pe.processedAt < :cutoffDate AND pe.status = 'SUCCESS'")
    int deleteOldSuccessfulEvents(@Param("cutoffDate") LocalDateTime cutoffDate);
}