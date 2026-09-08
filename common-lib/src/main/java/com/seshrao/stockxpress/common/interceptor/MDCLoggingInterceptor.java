package com.seshrao.stockxpress.common.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

/**
 * MDC (Mapped Diagnostic Context) Logging Interceptor for adding correlation IDs
 * and contextual information to logs.
 * 
 * This interceptor:
 * - Generates or extracts correlation IDs for request tracing
 * - Adds user information to MDC
 * - Adds request URI and method to MDC
 * - Cleans up MDC after request completion
 * 
 * MDC Keys:
 * - correlationId: Unique identifier for request tracking
 * - userId: User identifier (if available)
 * - requestUri: Request URI path
 * - requestMethod: HTTP method
 * - sessionId: Session identifier
 * 
 * @author Seshrao
 */
@Slf4j
@Component
public class MDCLoggingInterceptor implements HandlerInterceptor {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String SESSION_ID_HEADER = "X-Session-ID";
    
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_USER_ID = "userId";
    private static final String MDC_REQUEST_URI = "requestUri";
    private static final String MDC_REQUEST_METHOD = "requestMethod";
    private static final String MDC_SESSION_ID = "sessionId";
    private static final String MDC_CLIENT_IP = "clientIp";

    /**
     * Pre-handle: Set up MDC context before request processing.
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Generate or extract correlation ID
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.trim().isEmpty()) {
            correlationId = generateCorrelationId();
        }
        MDC.put(MDC_CORRELATION_ID, correlationId);
        
        // Add correlation ID to response header for client tracking
        response.setHeader(CORRELATION_ID_HEADER, correlationId);
        
        // Extract and add user ID if present
        String userId = request.getHeader(USER_ID_HEADER);
        if (userId != null && !userId.trim().isEmpty()) {
            MDC.put(MDC_USER_ID, userId);
        }
        
        // Extract and add session ID if present
        String sessionId = request.getHeader(SESSION_ID_HEADER);
        if (sessionId == null && request.getSession(false) != null) {
            sessionId = request.getSession(false).getId();
        }
        if (sessionId != null && !sessionId.trim().isEmpty()) {
            MDC.put(MDC_SESSION_ID, sessionId);
        }
        
        // Add request information
        MDC.put(MDC_REQUEST_URI, request.getRequestURI());
        MDC.put(MDC_REQUEST_METHOD, request.getMethod());
        
        // Add client IP address
        String clientIp = getClientIpAddress(request);
        if (clientIp != null && !clientIp.trim().isEmpty()) {
            MDC.put(MDC_CLIENT_IP, clientIp);
        }
        
        log.debug("MDC context initialized for request: {} {}", request.getMethod(), request.getRequestURI());
        
        return true;
    }

    /**
     * Post-handle: Log request completion (optional).
     */
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, 
                          Object handler, ModelAndView modelAndView) {
        log.debug("Request processing completed with status: {}", response.getStatus());
    }

    /**
     * After-completion: Clean up MDC context.
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                               Object handler, Exception ex) {
        if (ex != null) {
            log.error("Request completed with exception: {}", ex.getMessage(), ex);
        }
        
        log.debug("Clearing MDC context for request: {} {}", request.getMethod(), request.getRequestURI());
        
        // Clear all MDC entries
        MDC.remove(MDC_CORRELATION_ID);
        MDC.remove(MDC_USER_ID);
        MDC.remove(MDC_REQUEST_URI);
        MDC.remove(MDC_REQUEST_METHOD);
        MDC.remove(MDC_SESSION_ID);
        MDC.remove(MDC_CLIENT_IP);
        
        // Clear entire MDC to ensure no memory leaks
        MDC.clear();
    }

    /**
     * Generate a unique correlation ID.
     */
    private String generateCorrelationId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Extract client IP address from request, considering proxy headers.
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP",
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };

        for (String headerName : headerNames) {
            String ip = request.getHeader(headerName);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // X-Forwarded-For can contain multiple IPs, get the first one
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }

        return request.getRemoteAddr();
    }
}