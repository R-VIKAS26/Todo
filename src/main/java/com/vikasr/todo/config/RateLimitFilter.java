package com.vikasr.todo.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final boolean enabled;
    private final int requestsPerWindow;
    private final long windowMillis;
    private final String[] pathPrefixes;
    private final Clock clock;
    private final Map<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimitFilter(@Value("${app.rate-limit.enabled:false}") boolean enabled,
                           @Value("${app.rate-limit.requests-per-window:300}") int requestsPerWindow,
                           @Value("${app.rate-limit.window-seconds:60}") long windowSeconds,
                           @Value("${app.rate-limit.paths:/api/v1/}") String configuredPaths) {
        this.enabled = enabled;
        this.requestsPerWindow = requestsPerWindow;
        this.windowMillis = Math.max(1, windowSeconds) * 1000;
        this.pathPrefixes = Arrays.stream(configuredPaths.split("\\s*,\\s*"))
                .map(String::trim)
                .filter(path -> !path.isEmpty())
                .distinct()
                .toArray(String[]::new);
        this.clock = Clock.systemUTC();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled || requestsPerWindow <= 0 || pathPrefixes.length == 0) {
            return true;
        }

        String path = request.getRequestURI();
        for (String prefix : pathPrefixes) {
            if (path.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long now = clock.millis();
        String clientKey = resolveClientKey(request);
        WindowCounter counter = counters.compute(clientKey, (key, existing) -> refreshCounter(existing, now));

        int currentCount = counter.count.incrementAndGet();
        if (currentCount > requestsPerWindow) {
            long retryAfterSeconds = Math.max(1, (counter.windowStartedAt + windowMillis - now + 999) / 1000);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setHeader("Retry-After", Long.toString(retryAfterSeconds));
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write("Rate limit exceeded");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private WindowCounter refreshCounter(WindowCounter existing, long now) {
        if (existing == null || now - existing.windowStartedAt >= windowMillis) {
            return new WindowCounter(now);
        }
        return existing;
    }

    private String resolveClientKey(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }

    private static final class WindowCounter {
        private final long windowStartedAt;
        private final AtomicInteger count = new AtomicInteger();

        private WindowCounter(long windowStartedAt) {
            this.windowStartedAt = windowStartedAt;
        }
    }
}
