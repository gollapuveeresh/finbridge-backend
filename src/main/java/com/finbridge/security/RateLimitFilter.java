package com.finbridge.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Lightweight in-memory, per-IP fixed-window rate limiter for authentication endpoints.
 * Mitigates brute-force / credential-stuffing / auth DoS without external dependencies.
 *
 * For a multi-instance deployment, replace the in-memory map with a shared store (Redis)
 * or enforce limits at the edge (Cloudflare / API gateway).
 */
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    // Sensitive endpoints that must be throttled (exact path match).
    private static final Set<String> LIMITED = Set.of(
            "/api/auth/login", "/api/auth/register",
            "/api/auth/forgot-password", "/api/auth/reset-password",
            "/api/b2b/login", "/api/b2b/register");

    private static final int MAX_REQUESTS = 10;          // per window, per IP
    private static final long WINDOW_MS = 60_000;        // 1 minute
    private static final int MAX_TRACKED_KEYS = 50_000;  // memory guard

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    /**
     * Whether to trust the X-Forwarded-For header for the client IP. OFF by default: a client
     * can spoof XFF to rotate the rate-limit key and defeat the throttle. Enable ONLY when the
     * app sits behind a trusted reverse proxy that sets XFF (set app.ratelimit.trust-forwarded-header=true).
     */
    private final boolean trustForwardedHeader;

    public RateLimitFilter(@Value("${app.ratelimit.trust-forwarded-header:false}") boolean trustForwardedHeader) {
        this.trustForwardedHeader = trustForwardedHeader;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain chain) throws ServletException, IOException {
        if (!("POST".equalsIgnoreCase(request.getMethod()) && LIMITED.contains(request.getRequestURI()))) {
            chain.doFilter(request, response);
            return;
        }
        String key = clientIp(request) + "|" + request.getRequestURI();
        if (!allow(key)) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setHeader("Retry-After", "60");
            response.getWriter().write("{\"status\":\"error\",\"message\":\"Too many requests. Please try again later.\"}");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean allow(String key) {
        long now = System.currentTimeMillis();
        // Bound memory by evicting only already-expired windows — never flush live counters,
        // which would otherwise hand every IP a fresh quota the moment the map fills up.
        if (windows.size() > MAX_TRACKED_KEYS) windows.values().removeIf(w -> now >= w.resetAt);
        Window w = windows.compute(key, (k, v) -> {
            if (v == null || now >= v.resetAt) return new Window(now + WINDOW_MS);
            v.count++;
            return v;
        });
        return w.count <= MAX_REQUESTS;
    }

    private String clientIp(HttpServletRequest req) {
        if (trustForwardedHeader) {
            String xff = req.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        }
        return req.getRemoteAddr();
    }

    private static final class Window {
        final long resetAt;
        volatile int count = 1;
        Window(long resetAt) { this.resetAt = resetAt; }
    }
}
