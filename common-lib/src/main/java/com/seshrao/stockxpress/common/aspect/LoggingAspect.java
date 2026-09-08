package com.seshrao.stockxpress.common.aspect;

import com.seshrao.stockxpress.common.util.JsonUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect for logging method execution in service and controller layers.
 * This aspect uses @Around advice to log method entry, exit, and exceptions.
 * 
 * <p>Features:
 * <ul>
 *   <li>Logs method entry with parameters</li>
 *   <li>Logs method exit with return value</li>
 *   <li>Logs exceptions with full stack trace</li>
 *   <li>Provides context information (class, method name)</li>
 * </ul>
 * 
 * @author StockXpress Team
 * @version 1.0
 */
@Aspect
@Component
public class LoggingAspect {

    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);

    /**
     * Pointcut for all methods in service layer.
     */
    @Pointcut("execution(* com.seshrao.stockxpress..service..*(..))")
    public void serviceMethods() {
        // Pointcut definition
    }

    /**
     * Pointcut for all methods in controller layer.
     */
    @Pointcut("execution(* com.seshrao.stockxpress..controller..*(..))")
    public void controllerMethods() {
        // Pointcut definition
    }

    /**
     * Pointcut for all methods in repository layer.
     */
    @Pointcut("execution(* com.seshrao.stockxpress..repository..*(..))")
    public void repositoryMethods() {
        // Pointcut definition
    }

    /**
     * Around advice for logging method execution.
     * Logs method entry, exit, and any exceptions that occur.
     *
     * @param joinPoint the proceeding join point
     * @return the result of the method execution
     * @throws Throwable if the method throws an exception
     */
    @Around("serviceMethods() || controllerMethods()")
    public Object logMethodExecution(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = signature.getDeclaringType().getSimpleName();
        String methodName = signature.getName();
        Object[] args = joinPoint.getArgs();

        // Log method entry
        logger.info("Entering method: {}.{} with arguments: {}",
                className, methodName, formatArguments(args));

        long startTime = System.currentTimeMillis();
        Object result = null;

        try {
            // Proceed with method execution
            result = joinPoint.proceed();

            // Log method exit
            long executionTime = System.currentTimeMillis() - startTime;
            logger.info("Exiting method: {}.{} with result: {} | Execution time: {} ms",
                    className, methodName, formatResult(result), executionTime);

            return result;
        } catch (Exception e) {
            // Log exception
            long executionTime = System.currentTimeMillis() - startTime;
            logger.error("Exception in method: {}.{} after {} ms | Exception: {} | Message: {}",
                    className, methodName, executionTime, 
                    e.getClass().getSimpleName(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Formats method arguments for logging.
     * Converts arrays to string representation and handles null values.
     *
     * @param args the method arguments
     * @return formatted string representation of arguments
     */
    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "none";
        }
        
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(formatValue(args[i]));
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Formats method result for logging.
     *
     * @param result the method result
     * @return formatted string representation of result
     */
    private String formatResult(Object result) {
        return formatValue(result);
    }

    /**
     * Formats a single value for logging.
     * Handles null values, collections, arrays, and complex objects.
     *
     * @param value the value to format
     * @return formatted string representation
     */
    private String formatValue(Object value) {
        if (value == null) {
            return "null";
        }
        
        if (value instanceof String) {
            return "\"" + value + "\"";
        }
        
        if (value.getClass().isArray()) {
            return Arrays.toString((Object[]) value);
        }
        
        // For complex objects, use simple toString to avoid excessive logging
        String className = value.getClass().getSimpleName();
        return className + "@" + Integer.toHexString(value.hashCode());
    }
}
