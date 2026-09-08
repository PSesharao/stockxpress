package com.seshrao.stockxpress.common.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Utility class for JSON operations using Jackson.
 */
public final class JsonUtil {

    private static final Logger logger = LoggerFactory.getLogger(JsonUtil.class);
    private static final ObjectMapper objectMapper;

    static {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        objectMapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);
    }

    private JsonUtil() {
        throw new IllegalStateException("Utility class - cannot be instantiated");
    }

    /**
     * Gets the configured ObjectMapper instance.
     */
    public static ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    /**
     * Converts an object to JSON string.
     *
     * @param obj the object to convert
     * @return JSON string representation
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Error converting object to JSON: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts an object to pretty-printed JSON string.
     *
     * @param obj the object to convert
     * @return pretty-printed JSON string
     */
    public static String toPrettyJson(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            logger.error("Error converting object to pretty JSON: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts JSON string to an object of specified type.
     *
     * @param json  the JSON string
     * @param clazz the target class
     * @param <T>   the type parameter
     * @return the deserialized object
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (StringUtil.isBlank(json) || clazz == null) {
            return null;
        }
        try {
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            logger.error("Error converting JSON to object: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts JSON byte array to an object of specified type.
     *
     * @param jsonBytes the JSON byte array
     * @param clazz     the target class
     * @param <T>       the type parameter
     * @return the deserialized object
     */
    public static <T> T fromJson(byte[] jsonBytes, Class<T> clazz) {
        if (jsonBytes == null || jsonBytes.length == 0 || clazz == null) {
            return null;
        }
        try {
            return objectMapper.readValue(jsonBytes, clazz);
        } catch (IOException e) {
            logger.error("Error converting JSON bytes to object: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Converts an object to JSON byte array.
     *
     * @param obj the object to convert
     * @return JSON byte array
     */
    public static byte[] toJsonBytes(Object obj) {
        if (obj == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsBytes(obj);
        } catch (JsonProcessingException e) {
            logger.error("Error converting object to JSON bytes: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Clones an object by serializing and deserializing it.
     *
     * @param obj   the object to clone
     * @param clazz the class of the object
     * @param <T>   the type parameter
     * @return cloned object
     */
    public static <T> T clone(T obj, Class<T> clazz) {
        if (obj == null) {
            return null;
        }
        try {
            String json = objectMapper.writeValueAsString(obj);
            return objectMapper.readValue(json, clazz);
        } catch (IOException e) {
            logger.error("Error cloning object: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Checks if a string is valid JSON.
     *
     * @param json the string to check
     * @return true if valid JSON, false otherwise
     */
    public static boolean isValidJson(String json) {
        if (StringUtil.isBlank(json)) {
            return false;
        }
        try {
            objectMapper.readTree(json);
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}