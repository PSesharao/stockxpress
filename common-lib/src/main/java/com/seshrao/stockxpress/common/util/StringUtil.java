package com.seshrao.stockxpress.common.util;

import java.util.Collection;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Utility class for String operations.
 */
public final class StringUtil {

    private StringUtil() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );

    /**
     * Checks if a string is null or empty.
     */
    public static boolean isEmpty(String str) {
        return str == null || str.isEmpty();
    }

    /**
     * Checks if a string is not null and not empty.
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * Checks if a string is null, empty, or contains only whitespace.
     */
    public static boolean isBlank(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Checks if a string is not blank.
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * Returns default value if string is null or empty.
     */
    public static String defaultIfEmpty(String str, String defaultValue) {
        return isEmpty(str) ? defaultValue : str;
    }

    /**
     * Returns default value if string is null or blank.
     */
    public static String defaultIfBlank(String str, String defaultValue) {
        return isBlank(str) ? defaultValue : str;
    }

    /**
     * Trims a string, returning null if the result is empty.
     */
    public static String trimToNull(String str) {
        if (str == null) {
            return null;
        }
        String trimmed = str.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Trims a string, returning empty string if null.
     */
    public static String trimToEmpty(String str) {
        return str == null ? "" : str.trim();
    }

    /**
     * Capitalizes the first letter of a string.
     */
    public static String capitalize(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Converts a string to camelCase.
     */
    public static String toCamelCase(String str) {
        if (isEmpty(str)) {
            return str;
        }
        return str.substring(0, 1).toLowerCase() + str.substring(1);
    }

    /**
     * Joins a collection of strings with a delimiter.
     */
    public static String join(Collection<String> collection, String delimiter) {
        if (collection == null || collection.isEmpty()) {
            return "";
        }
        return String.join(delimiter, collection);
    }

    /**
     * Checks if a string contains only digits.
     */
    public static boolean isNumeric(String str) {
        if (isEmpty(str)) {
            return false;
        }
        return str.matches("\\d+");
    }

    /**
     * Checks if a string is a valid email format.
     */
    public static boolean isValidEmail(String email) {
        if (isEmpty(email)) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Generates a random UUID string.
     */
    public static String generateUUID() {
        return UUID.randomUUID().toString();
    }

    /**
     * Truncates a string to a maximum length.
     */
    public static String truncate(String str, int maxLength) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength);
    }

    /**
     * Truncates a string and adds ellipsis.
     */
    public static String truncateWithEllipsis(String str, int maxLength) {
        if (isEmpty(str) || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Masks a string by showing only the last n characters.
     */
    public static String maskExceptLast(String str, int visibleChars, char maskChar) {
        if (isEmpty(str) || str.length() <= visibleChars) {
            return str;
        }
        int maskLength = str.length() - visibleChars;
        StringBuilder masked = new StringBuilder();
        for (int i = 0; i < maskLength; i++) {
            masked.append(maskChar);
        }
        masked.append(str.substring(maskLength));
        return masked.toString();
    }

    /**
     * Converts null to empty string.
     */
    public static String nullToEmpty(String str) {
        return str == null ? "" : str;
    }

    /**
     * Converts empty string to null.
     */
    public static String emptyToNull(String str) {
        return isEmpty(str) ? null : str;
    }
}