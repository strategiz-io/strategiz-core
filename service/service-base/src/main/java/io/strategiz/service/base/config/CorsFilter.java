package io.strategiz.service.base.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component("serviceBaseCorsFilter")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        // Get the origin from the request
        String origin = request.getHeader("Origin");
        
        // Allow specific origins or patterns
        if (origin != null) {
            // Check exact matches first
            if (origin.equals("http://localhost:3000") ||
                origin.equals("http://localhost:3001") ||
                origin.equals("https://localhost:8443") ||
                origin.equals("https://strategiz.io") ||
                origin.equals("https://strategiz-io.web.app") ||
                origin.equals("https://strategiz-io.firebaseapp.com")) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials", "true");
            }
            // Check patterns
            else if (origin.matches("http://localhost:\\d+") ||
                     origin.matches("https://localhost:\\d+") ||
                     origin.matches("https://.*\\.strategiz\\.io") ||
                     origin.matches("https://.*\\.web\\.app") ||
                     origin.matches("https://.*\\.firebaseapp\\.com") ||
                     origin.matches("https://.*\\.run\\.app")) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials", "true");
            }
            // If origin doesn't match, don't set Access-Control-Allow-Origin header
            // This prevents the wildcard issue with credentials
        }
        
        // Set other CORS headers (always set these for preflight)
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE, PUT");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Headers", "x-requested-with, authorization, content-type, accept, origin, x-device-id, cookie, set-cookie");

        // Handle preflight requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            chain.doFilter(req, res);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void destroy() {
    }
}
