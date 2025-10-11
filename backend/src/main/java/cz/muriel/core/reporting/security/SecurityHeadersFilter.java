package cz.muriel.core.reporting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Security Headers Filter for reporting endpoints
 * 
 * Adds security headers:
 * - Content-Security-Policy
 * - X-Content-Type-Options: nosniff
 * - X-Frame-Options: DENY
 * - X-XSS-Protection: 1; mode=block
 * - Strict-Transport-Security (HSTS)
 * - Referrer-Policy
 * 
 * Applies to all /api/reports/** and /api/entities/** endpoints
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // Apply to reporting endpoints
        if (path.startsWith("/api/reports") || path.startsWith("/api/entities") || path.startsWith("/api/bulk-jobs")) {
            
            // Content Security Policy: Restrict resource loading
            response.setHeader("Content-Security-Policy", 
                "default-src 'self'; " +
                "script-src 'self'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data: https:; " +
                "font-src 'self'; " +
                "connect-src 'self'; " +
                "frame-ancestors 'none'");

            // Prevent MIME type sniffing
            response.setHeader("X-Content-Type-Options", "nosniff");

            // Prevent clickjacking
            response.setHeader("X-Frame-Options", "DENY");

            // XSS Protection (legacy browsers)
            response.setHeader("X-XSS-Protection", "1; mode=block");

            // HSTS: Force HTTPS for 1 year
            if (request.isSecure()) {
                response.setHeader("Strict-Transport-Security", 
                    "max-age=31536000; includeSubDomains; preload");
            }

            // Referrer Policy: Protect user privacy
            response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

            // Cache Control: No sensitive data caching
            if (path.contains("/query") || path.contains("/entities/")) {
                response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, private");
                response.setHeader("Pragma", "no-cache");
                response.setHeader("Expires", "0");
            }
        }

        filterChain.doFilter(request, response);
    }
}
