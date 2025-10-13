package io.strategiz.service.auth.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Utility class for managing authentication cookies
 * Implements industry-standard secure cookie practices
 */
@Component
public class CookieUtil {
    
    // Cookie names
    public static final String ACCESS_TOKEN_COOKIE = "strategiz-access-token";
    public static final String REFRESH_TOKEN_COOKIE = "strategiz-refresh-token";
    public static final String SESSION_ID_COOKIE = "strategiz-session";
    
    @Value("${app.cookie.secure:true}")
    private boolean secureCookie;
    
    @Value("${app.cookie.domain:}")
    private String cookieDomain;
    
    @Value("${app.cookie.access-token-max-age:1800}") // 30 minutes default
    private int accessTokenMaxAge;
    
    @Value("${app.cookie.refresh-token-max-age:604800}") // 7 days default
    private int refreshTokenMaxAge;

    @Value("${app.cookie.same-site:Lax}")
    private String sameSite;
    
    /**
     * Set access token cookie (short-lived, HTTP-only)
     */
    public void setAccessTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(ACCESS_TOKEN_COOKIE, token);
        cookie.setHttpOnly(true); // Prevent XSS attacks
        cookie.setSecure(secureCookie); // HTTPS only in production
        cookie.setPath("/");
        cookie.setMaxAge(accessTokenMaxAge);
        
        // Set SameSite attribute to prevent CSRF
        cookie.setAttribute("SameSite", sameSite);
        
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }
        
        response.addCookie(cookie);
    }
    
    /**
     * Set refresh token cookie (long-lived, HTTP-only, more restrictive)
     */
    public void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie(REFRESH_TOKEN_COOKIE, token);
        cookie.setHttpOnly(true); // Prevent XSS attacks
        cookie.setSecure(secureCookie); // HTTPS only in production
        cookie.setPath("/"); // Allow all paths to access refresh token
        cookie.setMaxAge(refreshTokenMaxAge);
        
        // Set SameSite for refresh token
        cookie.setAttribute("SameSite", sameSite);
        
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }
        
        response.addCookie(cookie);
    }
    
    /**
     * Set session ID cookie (for server-side session tracking)
     */
    public void setSessionCookie(HttpServletResponse response, String sessionId) {
        Cookie cookie = new Cookie(SESSION_ID_COOKIE, sessionId);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/");
        cookie.setMaxAge(-1); // Session cookie (expires when browser closes)
        
        // Set SameSite attribute
        cookie.setAttribute("SameSite", sameSite);
        
        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }
        
        response.addCookie(cookie);
    }
    
    /**
     * Clear all authentication cookies
     */
    public void clearAuthCookies(HttpServletResponse response) {
        clearCookie(response, ACCESS_TOKEN_COOKIE, "/");
        clearCookie(response, REFRESH_TOKEN_COOKIE, "/");
        clearCookie(response, SESSION_ID_COOKIE, "/");
    }
    
    /**
     * Clear a specific cookie
     * IMPORTANT: Must set the same attributes (Secure, SameSite) as when the cookie was created
     * otherwise modern browsers may not clear the cookie properly
     */
    private void clearCookie(HttpServletResponse response, String cookieName, String path) {
        Cookie cookie = new Cookie(cookieName, "");
        cookie.setMaxAge(0);
        cookie.setPath(path);
        cookie.setHttpOnly(true); // Must match original cookie attributes
        cookie.setSecure(secureCookie); // Must match original cookie attributes

        // Must set SameSite attribute to match original cookie
        cookie.setAttribute("SameSite", sameSite);

        if (cookieDomain != null && !cookieDomain.isEmpty()) {
            cookie.setDomain(cookieDomain);
        }

        response.addCookie(cookie);
    }
}