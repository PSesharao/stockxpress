package com.seshrao.stockxpress.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detailed validation error information for field-level validation failures.
 * 
 * @author StockXpress Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationError {

    /**
     * The name of the field that failed validation
     */
    private String field;

    /**
     * The rejected value
     */
    private Object rejectedValue;

    /**
     * The validation error message
     */
    private String message;

    /**
     * The validation constraint code (e.g., "NotNull", "Size", "Pattern")
     */
    private String code;

    /**
     * Creates a validation error with field and message
     * 
     * @param field Field name
     * @param message Error message
     * @return ValidationError instance
     */
    public static ValidationError of(String field, String message) {
        return ValidationError.builder()
                .field(field)
                .message(message)
                .build();
    }

    /**
     * Creates a full validation error
     * 
     * @param field Field name
     * @param rejectedValue The invalid value
     * @param message Error message
     * @param code Constraint code
     * @return ValidationError instance
     */
    public static ValidationError of(String field, Object rejectedValue, String message, String code) {
        return ValidationError.builder()
                .field(field)
                .rejectedValue(rejectedValue)
                .message(message)
                .code(code)
                .build();
    }
}