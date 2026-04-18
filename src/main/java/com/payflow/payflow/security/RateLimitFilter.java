package com.payflow.payflow.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.payflow.service.RateLimiterService;
import com.payflow.payflow.service.RateLimiterService.RateLimitResult;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String identifier = getIdentifier(request);

        // Single atomic call — check + increment together
        RateLimitResult result = rateLimiterService.checkAndIncrement(identifier);

        // Set headers on every response
        response.setHeader("X-RateLimit-Limit", "60");
        response.setHeader("X-RateLimit-Remaining", String.valueOf(result.remaining));
        response.setHeader("X-RateLimit-Reset", String.valueOf(result.resetSeconds));

        if (!result.allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);

            Map<String, Object> errorBody = new HashMap<>();
            errorBody.put("status", 429);
            errorBody.put("error", "Too Many Requests");
            errorBody.put("message", "Rate limit exceeded. Try again in "
                + result.resetSeconds + " seconds.");
            errorBody.put("retryAfter", result.resetSeconds);

            response.getWriter().write(objectMapper.writeValueAsString(errorBody));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getIdentifier(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                String[] parts = token.split("\\.");
                if (parts.length == 3) {
                    String payload = new String(
                        java.util.Base64.getUrlDecoder().decode(parts[1])
                    );
                    String sub = payload.split("\"sub\":\"")[1].split("\"")[0];
                    return "user:" + sub;
                }
            } catch (Exception ignored) {}
        }

        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        return "ip:" + ip;
    }
}