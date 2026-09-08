package com.seshrao.stockxpress.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardized error response DTO for consistent error handling across all services.
 * 
 * @author StockXpress Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    /**
     * ISO 8601 formatted timestamp when the error occurred
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime timestamp;

    /**
     * HTTP status code
     */
    private int status;

    /**
     * HTTP status text (e.g., "BAD_REQUEST", "NOT_FOUND")
     */
    private String error;

    /**
     * Main error message for end users
     */
    private String message;

    /**
     * Application-specific error code for programmatic handling
     */
    private String errorCode;

    /**
     * The request path that caused the error
     */
    private String path;

    /**
     * Detailed error messages or validation errors
     */
    private List<String> details;

    /**
     * Trace ID for distributed tracing (from Sleuth)
     */
    private String traceId;

    /**
     * Debug information (only included in non-production environments)
     */
    private String debugInfo;

    /**
     * Creates a simple error response with minimal information
     * 
     * @param status HTTP status code
     * @param error HTTP status text
     * @param message Error message
     * @param path Request path
     * @return ErrorResponse instance
     */
    public static ErrorResponse of(int status, String error, String message, String path) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status)
                .error(error)
                .message(message)
                .path(path)
                .build();
    }
}