package com.seshrao.stockxpress.common.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Async Configuration with custom thread pool executors for different operations.
 * 
 * Thread Pools:
 * - orderExecutor: For order processing operations
 * - inventoryExecutor: For inventory check operations
 * - eventExecutor: For event publishing operations
 * - defaultExecutor: Default async executor
 * 
 * @author Seshrao
 */
@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {

    // Order Executor Properties
    @Value("${async.executor.order.core-pool-size:5}")
    private int orderCorePoolSize;

    @Value("${async.executor.order.max-pool-size:10}")
    private int orderMaxPoolSize;

    @Value("${async.executor.order.queue-capacity:100}")
    private int orderQueueCapacity;

    @Value("${async.executor.order.thread-name-prefix:order-exec-}")
    private String orderThreadNamePrefix;

    // Inventory Executor Properties
    @Value("${async.executor.inventory.core-pool-size:3}")
    private int inventoryCorePoolSize;

    @Value("${async.executor.inventory.max-pool-size:8}")
    private int inventoryMaxPoolSize;

    @Value("${async.executor.inventory.queue-capacity:50}")
    private int inventoryQueueCapacity;

    @Value("${async.executor.inventory.thread-name-prefix:inventory-exec-}")
    private String inventoryThreadNamePrefix;

    // Event Executor Properties
    @Value("${async.executor.event.core-pool-size:2}")
    private int eventCorePoolSize;

    @Value("${async.executor.event.max-pool-size:5}")
    private int eventMaxPoolSize;

    @Value("${async.executor.event.queue-capacity:200}")
    private int eventQueueCapacity;

    @Value("${async.executor.event.thread-name-prefix:event-exec-}")
    private String eventThreadNamePrefix;

    // Default Executor Properties
    @Value("${async.executor.default.core-pool-size:4}")
    private int defaultCorePoolSize;

    @Value("${async.executor.default.max-pool-size:8}")
    private int defaultMaxPoolSize;

    @Value("${async.executor.default.queue-capacity:100}")
    private int defaultQueueCapacity;

    @Value("${async.executor.default.thread-name-prefix:async-exec-}")
    private String defaultThreadNamePrefix;

    @Value("${async.executor.await-termination-seconds:60}")
    private int awaitTerminationSeconds;

    /**
     * Thread pool executor for order processing operations.
     * Use @Async("orderExecutor") to use this executor.
     */
    @Bean(name = "orderExecutor")
    public Executor orderExecutor() {
        log.info("Initializing Order Executor with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                orderCorePoolSize, orderMaxPoolSize, orderQueueCapacity);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(orderCorePoolSize);
        executor.setMaxPoolSize(orderMaxPoolSize);
        executor.setQueueCapacity(orderQueueCapacity);
        executor.setThreadNamePrefix(orderThreadNamePrefix);
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        
        log.info("Order Executor initialized successfully");
        return executor;
    }

    /**
     * Thread pool executor for inventory operations.
     * Use @Async("inventoryExecutor") to use this executor.
     */
    @Bean(name = "inventoryExecutor")
    public Executor inventoryExecutor() {
        log.info("Initializing Inventory Executor with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                inventoryCorePoolSize, inventoryMaxPoolSize, inventoryQueueCapacity);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(inventoryCorePoolSize);
        executor.setMaxPoolSize(inventoryMaxPoolSize);
        executor.setQueueCapacity(inventoryQueueCapacity);
        executor.setThreadNamePrefix(inventoryThreadNamePrefix);
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        
        log.info("Inventory Executor initialized successfully");
        return executor;
    }

    /**
     * Thread pool executor for event publishing operations.
     * Use @Async("eventExecutor") to use this executor.
     */
    @Bean(name = "eventExecutor")
    public Executor eventExecutor() {
        log.info("Initializing Event Executor with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                eventCorePoolSize, eventMaxPoolSize, eventQueueCapacity);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(eventCorePoolSize);
        executor.setMaxPoolSize(eventMaxPoolSize);
        executor.setQueueCapacity(eventQueueCapacity);
        executor.setThreadNamePrefix(eventThreadNamePrefix);
        executor.setRejectedExecutionHandler(new AbortPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        
        log.info("Event Executor initialized successfully");
        return executor;
    }

    /**
     * Default async executor.
     * This executor is used when no specific executor is specified in @Async annotation.
     */
    @Override
    @Bean(name = "taskExecutor")
    public Executor getAsyncExecutor() {
        log.info("Initializing Default Async Executor with corePoolSize={}, maxPoolSize={}, queueCapacity={}",
                defaultCorePoolSize, defaultMaxPoolSize, defaultQueueCapacity);
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(defaultCorePoolSize);
        executor.setMaxPoolSize(defaultMaxPoolSize);
        executor.setQueueCapacity(defaultQueueCapacity);
        executor.setThreadNamePrefix(defaultThreadNamePrefix);
        executor.setRejectedExecutionHandler(new CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
        executor.initialize();
        
        log.info("Default Async Executor initialized successfully");
        return executor;
    }

    /**
     * Custom rejection policy that logs and runs the task in the caller's thread.
     */
    private static class CallerRunsPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.warn("Task {} rejected from executor {}. Running in caller's thread.", 
                    r.toString(), executor.toString());
            if (!executor.isShutdown()) {
                r.run();
            }
        }
    }

    /**
     * Custom rejection policy that aborts the task and logs the rejection.
     */
    private static class AbortPolicy implements RejectedExecutionHandler {
        @Override
        public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
            log.error("Task {} rejected from executor {}. Task will be aborted.", 
                    r.toString(), executor.toString());
            throw new java.util.concurrent.RejectedExecutionException(
                    "Task " + r.toString() + " rejected from " + executor.toString());
        }
    }
}