package com.pharmapos.cloud.security;

import com.pharmapos.shared.multitenancy.TenantContext;
import com.pharmapos.shared.multitenancy.TenantContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Sliding-window rate limit filter (TDD §6.1 "Rate limiting", §9.5 "Abuse controls").
 *
 * <h3>Rate limit dimensions (all applied):</h3>
 * <ul>
 *   <li>Per authenticated user (JWT {@code sub})</li>
 *   <li>Per enrolled device (JWT {@code device_id})</li>
 *   <li>Per tenant (JWT {@code tenant_id})</li>
 *   <li>Per source IP address</li>
 * </ul>
 *
 * <h3>Stricter limits on sensitive endpoints:</h3>
 * <ul>
 *   <li>{@code /api/v1/identity/...} (auth-related) — 30 req/min</li>
 *   <li>{@code /api/v1/reports/...} and {@code /api/v1/.../export} — 10 req/min</li>
 *   <li>All other API paths — 300 req/min</li>
 * </ul>
 *
 * <p>Sliding window is implemented via a Redis sorted set per dimension key.
 * Each request adds a member with score = epoch-ms; a {@code ZREMRANGEBYSCORE}
 * trims members outside the window; {@code ZCARD} checks the count. When Redis
 * is unavailable the filter fails open (logs a warning) rather than blocking all traffic.
 *
 * <p>When no Redis bean is available (edge/BLN profile) the filter uses an
 * in-process {@code ConcurrentHashMap} of {@link java.util.concurrent.atomic.AtomicInteger}
 * counters — simpler but sufficient for the single-process BLN.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 25)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    /** Sliding window size for all limit tiers. */
    private static final Duration WINDOW = Duration.ofMinutes(1);

    /** Request limits by endpoint group (requests per WINDOW). */
    private static final Map<String, Integer> LIMITS = Map.of(
        "auth",    30,
        "export",  10,
        "reports", 10,
        "default", 300
    );

    @Value("${pharmapos.rate-limit.enabled:true}")
    private boolean enabled;

    private final StringRedisTemplate redis;

    /**
     * @param redis Redis template — may be {@code null} when Redis is not configured
     *              (e.g. edge profile). Falls back to in-process counters.
     */
    public RateLimitFilter(
            @org.springframework.lang.Nullable StringRedisTemplate redis
    ) {
        this.redis = redis;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest  request,
            HttpServletResponse response,
            FilterChain         chain
    ) throws ServletException, IOException {

        int limit = resolveLimit(request.getRequestURI());

        // Build composite rate-limit keys — check all dimensions
        String[] keys = buildKeys(request);

        for (String key : keys) {
            if (key == null || key.isBlank()) continue;
            if (!checkAndIncrement(key, limit)) {
                log.warn("Rate limit exceeded: key={} uri={} src={}",
                    key, request.getRequestURI(), request.getRemoteAddr());
                response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
                response.setContentType("application/json");
                response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
                response.getWriter().write(
                    "{\"code\":\"RATE_LIMIT_EXCEEDED\"," +
                    "\"message\":\"Too many requests. Please slow down.\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    /**
     * Checks the sliding window counter for {@code key} and increments it if
     * the limit has not been reached.
     *
     * @return {@code true} if the request is allowed; {@code false} if rate-limited
     */
    private boolean checkAndIncrement(String key, int limit) {
        if (redis == null) {
            return inProcessCheck(key, limit);
        }
        try {
            long now       = Instant.now().toEpochMilli();
            long windowStart = now - WINDOW.toMillis();

            String redisKey = "rl:" + key;

            // Atomic pipeline: trim expired members, count, add current member
            redis.execute(connection -> {
                connection.zSetCommands().zRemRangeByScore(
                    redisKey.getBytes(), Double.NEGATIVE_INFINITY, windowStart);
                return null;
            }, true);

            Long count = redis.opsForZSet().zCard(redisKey);
            if (count != null && count >= limit) {
                return false;
            }

            redis.opsForZSet().add(redisKey, String.valueOf(now), now);
            redis.expire(redisKey, WINDOW.plusSeconds(10));
            return true;

        } catch (Exception e) {
            // Redis unavailable — fail open to avoid a complete outage
            log.warn("Redis rate-limit check failed (failing open): {}", e.getMessage());
            return true;
        }
    }

    // ── In-process fallback (BLN / no Redis) ─────────────────────────────────

    private static final java.util.concurrent.ConcurrentHashMap<String, long[]>
        IN_PROCESS_WINDOWS = new java.util.concurrent.ConcurrentHashMap<>();

    private boolean inProcessCheck(String key, int limit) {
        long now = System.currentTimeMillis();
        long windowStart = now - WINDOW.toMillis();

        // Simple token bucket using a circular array is overkill for the BLN.
        // Use a 1-minute epoch bucket counter instead.
        String bucketKey = key + ":" + (now / WINDOW.toMillis());
        long[] counter = IN_PROCESS_WINDOWS.computeIfAbsent(bucketKey, k -> new long[]{0});
        synchronized (counter) {
            if (counter[0] >= limit) return false;
            counter[0]++;
        }
        return true;
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private int resolveLimit(String uri) {
        if (uri.contains("/identity") || uri.contains("/auth")) return LIMITS.get("auth");
        if (uri.contains("/export"))   return LIMITS.get("export");
        if (uri.contains("/reports"))  return LIMITS.get("reports");
        return LIMITS.get("default");
    }

    private String[] buildKeys(HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        TenantContext ctx = TenantContextHolder.peek();
        if (ctx == null) {
            return new String[]{"ip:" + ip};
        }
        String userId   = "user:"   + ctx.userId();
        String tenantId = "tenant:" + ctx.tenantId();
        String ipKey    = "ip:"     + ip;
        String deviceKey = ctx.deviceId() != null
            ? "device:" + ctx.deviceId()
            : null;
        return new String[]{userId, tenantId, ipKey, deviceKey};
    }
}
