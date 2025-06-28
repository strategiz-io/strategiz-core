package io.strategiz.framework.exception;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

/**
 * Servlet filter that sets up request context information for logging.
 * This filter runs at the highest precedence to ensure context is set
 * as early as possible in the request lifecycle.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter implements Filter {
    private static final String REQUEST_ID = "requestId";
    private static final String REQUEST_PATH = "requestPath";
    private static final String REQUEST_METHOD = "requestMethod";
    private static final String REQUEST_IP = "requestIp";
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        try {
            if (request instanceof HttpServletRequest) {
                HttpServletRequest httpRequest = (HttpServletRequest) request;
                
                // Generate a unique request ID
                String requestId = UUID.randomUUID().toString();
                
                // Get request path and method
                String path = httpRequest.getRequestURI();
                String method = httpRequest.getMethod();
                String ip = httpRequest.getRemoteAddr();
                
                // Store in MDC for logging frameworks
                MDC.put(REQUEST_ID, requestId);
                MDC.put(REQUEST_PATH, path);
                MDC.put(REQUEST_METHOD, method);
                MDC.put(REQUEST_IP, ip);
                
                // Store in our logging context
                LoggingContext.put(REQUEST_ID, requestId);
                LoggingContext.put(REQUEST_PATH, path);
                LoggingContext.put(REQUEST_METHOD, method);
                LoggingContext.put(REQUEST_IP, ip);
            }
            
            // Continue with request processing
            chain.doFilter(request, response);
        } finally {
            // Clean up
            MDC.clear();
            LoggingContext.clear();
        }
    }
}
