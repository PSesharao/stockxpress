package com.seshrao.stockxpress.common.util;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Utility class for validation operations.
 */
public final class ValidationUtil {

    private ValidationUtil() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$"
    );

    /**
     * Validates that an object is not null.
     */
    public static void requireNonNull(Object obj, String fieldName) {
        if (obj == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
    }

    /**
     * Validates that a string is not null or empty.
     */
    public static void requireNonEmpty(String str, String fieldName) {
        if (StringUtil.isEmpty(str)) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    /**
     * Validates that a string is not null, empty, or blank.
     */
    public static void requireNonBlank(String str, String fieldName) {
        if (StringUtil.isBlank(str)) {
            throw new IllegalArgumentException(fieldName + " cannot be null, empty, or blank");
        }
    }

    /**
     * Validates that a collection is not null or empty.
     */
    public static void requireNonEmpty(Collection<?> collection, String fieldName) {
        if (collection == null || collection.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " cannot be null or empty");
        }
    }

    /**
     * Validates that a number is positive.
     */
    public static void requirePositive(Number number, String fieldName) {
        if (number == null || number.doubleValue() <= 0) {
            throw new IllegalArgumentException(fieldName + " must be positive");
        }
    }

    /**
     * Validates that a number is non-negative.
     */
    public static void requireNonNegative(Number number, String fieldName) {
        if (number == null || number.doubleValue() < 0) {
            throw new IllegalArgumentException(fieldName + " must be non-negative");
        }
    }

    /**
     * Validates that a value is within a range.
     */
    public static void requireInRange(int value, int min, int max, String fieldName) {
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    String.format("%s must be between %d and %d", fieldName, min, max)
            );
        }
    }

    /**
     * Validates that a value is within a range (BigDecimal).
     */
    public static void requireInRange(BigDecimal value, BigDecimal min, BigDecimal max, String fieldName) {
        if (value == null || value.compareTo(min) < 0 || value.compareTo(max) > 0) {
            throw new IllegalArgumentException(
                    String.format("%s must be between %s and %s", fieldName, min, max)
            );
        }
    }

    /**
     * Validates email format.
     */
    public static void requireValidEmail(String email, String fieldName) {
        if (!StringUtil.isValidEmail(email)) {
            throw new IllegalArgumentException(fieldName + " must be a valid email address");
        }
    }

    /**
     * Validates phone number format.
     */
    public static boolean isValidPhone(String phone) {
        if (StringUtil.isEmpty(phone)) {
            return false;
        }
        return PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Validates that a string matches a pattern.
     */
    public static void requirePattern(String str, Pattern pattern, String fieldName) {
        if (str == null || !pattern.matcher(str).matches()) {
            throw new IllegalArgumentException(fieldName + " does not match required pattern");
        }
    }

    /**
     * Validates that a string length is within bounds.
     */
    public static void requireLength(String str, int minLength, int maxLength, String fieldName) {
        if (str == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        if (str.length() < minLength || str.length() > maxLength) {
            throw new IllegalArgumentException(
                    String.format("%s length must be between %d and %d characters", fieldName, minLength, maxLength)
            );
        }
    }

    /**
     * Validates that a collection size is within bounds.
     */
    public static void requireSize(Collection<?> collection, int minSize, int maxSize, String fieldName) {
        if (collection == null) {
            throw new IllegalArgumentException(fieldName + " cannot be null");
        }
        if (collection.size() < minSize || collection.size() > maxSize) {
            throw new IllegalArgumentException(
                    String.format("%s size must be between %d and %d", fieldName, minSize, maxSize)
            );
        }
    }

    /**
     * Checks if an object is null or empty (works with String, Collection, Map, Array).
     */
    public static boolean isNullOrEmpty(Object obj) {
        if (obj == null) {
            return true;
        }
        if (obj instanceof String) {
            return ((String) obj).isEmpty();
        }
        if (obj instanceof Collection) {
            return ((Collection<?>) obj).isEmpty();
        }
        if (obj instanceof Map) {
            return ((Map<?, ?>) obj).isEmpty();
        }
        if (obj.getClass().isArray()) {
            return ((Object[]) obj).length == 0;
        }
        return false;
    }

    /**
     * Validates that a condition is true.
     */
    public static void requireTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Validates that a condition is false.
     */
    public static void requireFalse(boolean condition, String message) {
        if (condition) {
            throw new IllegalArgumentException(message);
        }
    }
}