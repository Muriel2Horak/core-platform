package cz.muriel.core.reporting.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Content-Type Validation Filter
 * 
 * Enforces strict Content-Type validation for POST/PUT/PATCH requests:
 * - Only application/json allowed for API endpoints
 * - Prevents content-type confusion attacks
 * - Returns 415 Unsupported Media Type if invalid
 * 
 * OWASP Compliance:
 * - A3:2021 - Injection (prevents XML/file upload attacks)
 * - A8:2021 - Software and Data Integrity Failures
 */
@Slf4j
@Component
public class ContentTypeValidationFilter extends OncePerRequestFilter {

    private static final List<String> METHODS_WITH_BODY = Arrays.asList("POST", "PUT", "PATCH");
    private static final String ALLOWED_CONTENT_TYPE = "application/json";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();

        // Apply to reporting endpoints with request body
        if ((path.startsWith("/api/reports") || path.startsWith("/api/entities") || path.startsWith("/api/bulk-jobs"))
                && METHODS_WITH_BODY.contains(method)) {

            String contentType = request.getContentType();

            // Content-Type is required for POST/PUT/PATCH
            if (contentType == null || contentType.isEmpty()) {
                log.warn("Content-Type header missing for {} {}", method, path);
                sendError(response, HttpServletResponse.SC_BAD_REQUEST, 
                    "Content-Type header is required");
                return;
            }

            // Extract base content type (remove charset)
            String baseContentType = contentType.split(";")[0].trim();

            // Only application/json is allowed
            if (!baseContentType.equalsIgnoreCase(ALLOWED_CONTENT_TYPE)) {
                log.warn("Invalid Content-Type: {} for {} {} (expected: {})", 
                    baseContentType, method, path, ALLOWED_CONTENT_TYPE);
                sendError(response, HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE,
                    "Content-Type must be application/json");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private void sendError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        String errorJson = String.format("{\"error\":\"%s\",\"message\":\"%s\"}", 
            getStatusText(status), message);
        response.getWriter().write(errorJson);
    }

    private String getStatusText(int status) {
        return switch (status) {
            case HttpServletResponse.SC_BAD_REQUEST -> "Bad Request";
            case HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE -> "Unsupported Media Type";
            default -> "Error";
        };
    }
}
