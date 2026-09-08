package com.seshrao.stockxpress.common.constants;

/**
 * Application-wide constants for StockXpress microservices.
 * 
 * @author StockXpress Team
 */
public final class ApplicationConstants {

    private ApplicationConstants() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    /**
     * API version constants
     */
    public static final class Api {
        public static final String VERSION_V1 = "/api/v1";
        public static final String PRODUCT_PATH = "/api/product";
        public static final String INVENTORY_PATH = "/api/inventory";
        public static final String ORDER_PATH = "/api/order";
        public static final String NOTIFICATION_PATH = "/api/notification";
    }

    /**
     * HTTP header constants
     */
    public static final class Headers {
        public static final String CORRELATION_ID = "X-Correlation-ID";
        public static final String USER_ID = "X-User-ID";
        public static final String TRACE_ID = "X-B3-TraceId";
        public static final String SPAN_ID = "X-B3-SpanId";
        public static final String API_VERSION = "X-API-Version";
    }

    /**
     * Pagination constants
     */
    public static final class Pagination {
        public static final int DEFAULT_PAGE_SIZE = 20;
        public static final int MAX_PAGE_SIZE = 100;
        public static final String DEFAULT_SORT_BY = "createdAt";
        public static final String DEFAULT_SORT_DIRECTION = "DESC";
    }

    /**
     * Cache names
     */
    public static final class CacheNames {
        public static final String PRODUCTS = "products";
        public static final String PRODUCT_BY_ID = "productById";
        public static final String INVENTORY = "inventory";
        public static final String INVENTORY_BY_SKU = "inventoryBySku";
    }

    /**
     * Cache TTL in seconds
     */
    public static final class CacheTTL {
        public static final long PRODUCTS = 300; // 5 minutes
        public static final long INVENTORY = 10; // 10 seconds
        public static final long SHORT = 30; // 30 seconds
        public static final long MEDIUM = 300; // 5 minutes
        public static final long LONG = 3600; // 1 hour
    }

    /**
     * Error codes for consistent error handling
     */
    public static final class ErrorCodes {
        // Validation errors (4xx)
        public static final String VALIDATION_ERROR = "ERR_VALIDATION";
        public static final String INVALID_INPUT = "ERR_INVALID_INPUT";
        public static final String MISSING_REQUIRED_FIELD = "ERR_MISSING_FIELD";
        
        // Resource errors (4xx)
        public static final String RESOURCE_NOT_FOUND = "ERR_NOT_FOUND";
        public static final String RESOURCE_ALREADY_EXISTS = "ERR_ALREADY_EXISTS";
        
        // Business logic errors (4xx)
        public static final String INSUFFICIENT_INVENTORY = "ERR_INSUFFICIENT_INVENTORY";
        public static final String INVALID_ORDER_STATE = "ERR_INVALID_ORDER_STATE";
        public static final String BUSINESS_RULE_VIOLATION = "ERR_BUSINESS_RULE";
        
        // Service errors (5xx)
        public static final String SERVICE_UNAVAILABLE = "ERR_SERVICE_UNAVAILABLE";
        public static final String INTERNAL_ERROR = "ERR_INTERNAL";
        public static final String DATABASE_ERROR = "ERR_DATABASE";
        public static final String EXTERNAL_SERVICE_ERROR = "ERR_EXTERNAL_SERVICE";
    }

    /**
     * Service names for inter-service communication
     */
    public static final class ServiceNames {
        public static final String PRODUCT_SERVICE = "product-service";
        public static final String INVENTORY_SERVICE = "inventory-service";
        public static final String ORDER_SERVICE = "order-service";
        public static final String NOTIFICATION_SERVICE = "notification-service";
        public static final String API_GATEWAY = "api-gateway";
        public static final String DISCOVERY_SERVER = "discovery-server";
    }

    /**
     * Kafka topic names
     */
    public static final class KafkaTopics {
        public static final String ORDER_PLACED = "order-placed-events";
        public static final String ORDER_CONFIRMED = "order-confirmed-events";
        public static final String ORDER_CANCELLED = "order-cancelled-events";
        public static final String INVENTORY_RESERVED = "inventory-reserved-events";
        public static final String INVENTORY_RELEASED = "inventory-released-events";
        public static final String NOTIFICATION_TOPIC = "notificationTopic";
    }

    /**
     * Thread pool configuration
     */
    public static final class ThreadPool {
        public static final int ORDER_CORE_POOL_SIZE = 10;
        public static final int ORDER_MAX_POOL_SIZE = 20;
        public static final int ORDER_QUEUE_CAPACITY = 500;
        public static final int INVENTORY_CORE_POOL_SIZE = 5;
        public static final int INVENTORY_MAX_POOL_SIZE = 10;
        public static final int INVENTORY_QUEUE_CAPACITY = 100;
    }

    /**
     * Validation constants
     */
    public static final class Validation {
        public static final int PRODUCT_NAME_MIN_LENGTH = 3;
        public static final int PRODUCT_NAME_MAX_LENGTH = 100;
        public static final int PRODUCT_DESC_MAX_LENGTH = 500;
        public static final String PRICE_MIN = "0.01";
        public static final String PRICE_MAX = "999999.99";
        public static final int SKU_CODE_MIN_LENGTH = 3;
        public static final int SKU_CODE_MAX_LENGTH = 50;
        public static final int QUANTITY_MIN = 1;
        public static final int QUANTITY_MAX = 10000;
    }
}