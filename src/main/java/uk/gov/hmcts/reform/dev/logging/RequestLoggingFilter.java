package uk.gov.hmcts.reform.dev.logging;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Filter that logs all HTTP requests and responses with detailed information.
 * Adds MDC context for request correlation.
 */
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";
    private static final String METHOD = "method";
    private static final String PATH = "path";
    private static final String CLIENT_IP = "clientIp";
    private static final String STATUS_CODE = "statusCode";
    private static final String DURATION = "duration";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip logging for static resources and actuator endpoints
        String uri = request.getRequestURI();
        if (shouldSkipLogging(uri)) {
            filterChain.doFilter(request, response);
            return;
        }

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        String requestId = UUID.randomUUID().toString().substring(0, 8);
        long startTime = System.currentTimeMillis();

        try {
            // Set MDC context
            MDC.put(REQUEST_ID, requestId);
            MDC.put(METHOD, request.getMethod());
            MDC.put(PATH, uri);
            MDC.put(CLIENT_IP, getClientIp(request));

            logRequestStart(requestWrapper, requestId);

            filterChain.doFilter(requestWrapper, responseWrapper);

            long duration = System.currentTimeMillis() - startTime;
            MDC.put(STATUS_CODE, String.valueOf(responseWrapper.getStatus()));
            MDC.put(DURATION, String.valueOf(duration));

            logRequestComplete(requestWrapper, responseWrapper, duration, requestId);

        } finally {
            // Copy response body to actual response
            responseWrapper.copyBodyToResponse();
            // Clear MDC
            MDC.clear();
        }
    }

    private boolean shouldSkipLogging(String uri) {
        return uri.startsWith("/swagger")
            || uri.startsWith("/v3/api-docs")
            || uri.startsWith("/actuator")
            || uri.endsWith(".css")
            || uri.endsWith(".js")
            || uri.endsWith(".ico");
    }

    private void logRequestStart(ContentCachingRequestWrapper request, String requestId) {
        String queryString = request.getQueryString();
        String fullPath = queryString != null ? request.getRequestURI() + "?" + queryString : request.getRequestURI();

        log.info(">>> Request started: {} {} | RequestId: {} | Client: {} | User-Agent: {}",
            request.getMethod(),
            fullPath,
            requestId,
            getClientIp(request),
            request.getHeader("User-Agent")
        );

        // Log request headers at debug level
        log.debug("Request headers: Content-Type={}, Accept={}, Content-Length={}",
            request.getContentType(),
            request.getHeader("Accept"),
            request.getContentLength()
        );
    }

    private void logRequestComplete(ContentCachingRequestWrapper request,
                                    ContentCachingResponseWrapper response,
                                    long duration,
                                    String requestId) {
        int status = response.getStatus();
        String logLevel = status >= 500 ? "ERROR" : (status >= 400 ? "WARN" : "INFO");

        String message = String.format("<<< Request completed: %s %s | Status: %d | Duration: %dms | RequestId: %s",
            request.getMethod(),
            request.getRequestURI(),
            status,
            duration,
            requestId
        );

        if (status >= 500) {
            log.error(message);
            logResponseBody(response);
        } else if (status >= 400) {
            log.warn(message);
            logResponseBody(response);
        } else {
            log.info(message);
        }

        // Log performance warning for slow requests
        if (duration > 1000) {
            log.warn("Slow request detected: {} {} took {}ms", request.getMethod(), request.getRequestURI(), duration);
        }
    }

    private void logResponseBody(ContentCachingResponseWrapper response) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0 && content.length < 2000) {
            String body = new String(content, StandardCharsets.UTF_8);
            log.debug("Response body: {}", body);
        }
    }

    private String getClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isEmpty()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isEmpty()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }
}
