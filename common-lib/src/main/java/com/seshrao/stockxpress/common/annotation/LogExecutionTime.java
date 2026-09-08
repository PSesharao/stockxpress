package com.seshrao.stockxpress.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Custom annotation to mark methods for execution time logging.
 * When a method is annotated with @LogExecutionTime, the PerformanceLoggingAspect
 * will automatically log the execution time of that method.
 * 
 * <p>Usage example:
 * <pre>
 * \@LogExecutionTime
 * public void processOrder(Order order) {
 *     // method implementation
 * }
 * </pre>
 * 
 * @author StockXpress Team
 * @version 1.0
 * @see com.seshrao.stockxpress.common.aspect.PerformanceLoggingAspect
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface LogExecutionTime {
    
    /**
     * Optional description for the logged operation.
     * If not provided, the method name will be used.
     *
     * @return the description of the operation being logged
     */
    String value() default "";
    
    /**
     * Whether to log method parameters.
     * Default is false to avoid logging sensitive data.
     *
     * @return true to log parameters, false otherwise
     */
    boolean logParameters() default false;
    
    /**
     * Whether to log the return value.
     * Default is false to avoid logging sensitive data.
     *
     * @return true to log return value, false otherwise
     */
    boolean logReturnValue() default false;
    
    /**
     * Threshold in milliseconds for logging warnings.
     * If execution time exceeds this threshold, a warning will be logged.
     * Default is 0 (no threshold).
     *
     * @return the warning threshold in milliseconds
     */
    long warnThresholdMillis() default 0;
}
