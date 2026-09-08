package com.seshrao.stockxpress.common.aspect;

import com.seshrao.stockxpress.common.annotation.LogExecutionTime;
import com.seshrao.stockxpress.common.util.JsonUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
 * Aspect for tracking and logging method execution time.
 * This aspect intercepts methods annotated with @LogExecutionTime
 * and logs their execution duration.
 * 
 * <p>Features:
 * <ul>
 *   <li>Measures and logs method execution time</li>
 *   <li>Supports custom descriptions</li>
 *   <li>Optional parameter and return value logging</li>
 *   <li>Warning threshold for slow operations</li>
 *   <li>Performance metrics tracking</li>
 * </ul>
 * 
 * @author StockXpress Team
 * @version 1.0
 * @see LogExecutionTime
 */
@Aspect
@Component
public class PerformanceLoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(PerformanceLoggingAspect.class);
    private static final Logger performanceLogger = LoggerFactory.getLogger("performance");

    /**
     * Around advice for methods annotated with @LogExecutionTime.
     * Measures execution time and logs performance metrics.
     *
     * @param joinPoint the proceeding join point
     * @param logExecutionTime the annotation instance
     * @return the result of the method execution
     * @throws Throwable if the method throws an exception
     */
    @Around("@annotation(logExecutionTime)")
    public Object logExecutionTime(ProceedingJoinPoint joinPoint, LogExecutionTime logExecutionTime) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        String description = getDescription(logExecutionTime, methodName);

        long startTime = System.currentTimeMillis();
        Object result = null;
        boolean success = true;

        try {
            // Log entry with parameters if enabled
            if (logExecutionTime.logParameters()) {
                logger.debug("[PERFORMANCE] Starting: {} with parameters: {}",
                        description, formatParameters(joinPoint.getArgs()));
            } else {
                logger.debug("[PERFORMANCE] Starting: {}", description);
            }

            // Execute the method
            result = joinPoint.proceed();

            return result;
        } catch (Throwable throwable) {
            success = false;
            throw throwable;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            logPerformance(className, methodName, description, executionTime, 
                    logExecutionTime, result, success);
        }
    }

    /**
     * Around advice for classes annotated with @LogExecutionTime.
     * Applies performance logging to all public methods in the class.
     *
     * @param joinPoint the proceeding join point
     * @return the result of the method execution
     * @throws Throwable if the method throws an exception
     */
    @Around("@within(com.seshrao.stockxpress.common.annotation.LogExecutionTime) && execution(public * *(..))")
    public Object logClassExecutionTime(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();

        long startTime = System.currentTimeMillis();
        boolean success = true;

        try {
            logger.debug("[PERFORMANCE] Starting: {}.{}", className, methodName);
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            success = false;
            throw throwable;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;
            logSimplePerformance(className, methodName, executionTime, success);
        }
    }

    /**
     * Logs performance metrics with full details.
     *
     * @param className the class name
     * @param methodName the method name
     * @param description the operation description
     * @param executionTime the execution time in milliseconds
     * @param annotation the annotation instance
     * @param result the method result
     * @param success whether the method executed successfully
     */
    private void logPerformance(String className, String methodName, String description,
                                long executionTime, LogExecutionTime annotation,
                                Object result, boolean success) {
        // Determine log level based on threshold and success
        long threshold = annotation.warnThresholdMillis();
        boolean exceedsThreshold = threshold > 0 && executionTime > threshold;

        // Build log message
        StringBuilder message = new StringBuilder();
        message.append(String.format("[PERFORMANCE] Completed: %s | ", description));
        message.append(String.format("Time: %d ms | ", executionTime));
        message.append(String.format("Method: %s.%s | ", className, methodName));
        message.append(String.format("Status: %s", success ? "SUCCESS" : "FAILED"));

        // Add return value if enabled
        if (success && annotation.logReturnValue() && result != null) {
            message.append(String.format(" | Result: %s", formatReturnValue(result)));
        }

        // Log based on threshold and success
        if (!success) {
            logger.error(message.toString());
            performanceLogger.error(message.toString());
        } else if (exceedsThreshold) {
            logger.warn(message.toString() + String.format(" | Exceeded threshold of %d ms", threshold));
            performanceLogger.warn(message.toString());
        } else {
            logger.info(message.toString());
            performanceLogger.info(message.toString());
        }
    }

    /**
     * Logs simple performance metrics without annotation details.
     *
     * @param className the class name
     * @param methodName the method name
     * @param executionTime the execution time in milliseconds
     * @param success whether the method executed successfully
     */
    private void logSimplePerformance(String className, String methodName,
                                      long executionTime, boolean success) {
        String message = String.format("[PERFORMANCE] %s.%s | Time: %d ms | Status: %s",
                className, methodName, executionTime, success ? "SUCCESS" : "FAILED");

        if (success) {
            logger.info(message);
            performanceLogger.info(message);
        } else {
            logger.error(message);
            performanceLogger.error(message);
        }
    }

    /**
     * Gets the description from annotation or uses method name as fallback.
     *
     * @param annotation the annotation instance
     * @param methodName the method name
     * @return the description
     */
    private String getDescription(LogExecutionTime annotation, String methodName) {
        String value = annotation.value();
        return (value == null || value.trim().isEmpty()) ? methodName : value;
    }

    /**
     * Formats method parameters for logging.
     *
     * @param args the method arguments
     * @return formatted string representation
     */
    private String formatParameters(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof String) {
                sb.append("\"").append(arg).append("\"");
            } else {
                sb.append(arg.getClass().getSimpleName());
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Formats return value for logging.
     *
     * @param result the return value
     * @return formatted string representation
     */
    private String formatReturnValue(Object result) {
        if (result == null) {
            return "null";
        }
        if (result instanceof String) {
            return "\"" + result + "\"";
        }
        if (result instanceof Number || result instanceof Boolean) {
            return result.toString();
        }
        // For complex objects, just show the type
        return result.getClass().getSimpleName();
    }
}
