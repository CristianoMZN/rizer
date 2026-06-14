package br.com.rizermarketplaces.core.marketplace.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limit simples, in-memory, por IP + chave (rota).
 * Para produção, substitua por Bucket4j com Redis ou spring-cloud-gateway.
 *
 * Defaults (env-driven):
 *  - app.ratelimit.auth.capacity=5  por minuto (rotas /auth/**)
 *  - app.ratelimit.admin.capacity=60  por minuto (/admin/**)
 *  - app.ratelimit.global.capacity=300 por minuto (demais)
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class RateLimitFilter extends OncePerRequestFilter {

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int authCapacity;
    private final int adminCapacity;
    private final int globalCapacity;

    public RateLimitFilter(
        @org.springframework.beans.factory.annotation.Value("${app.ratelimit.auth.capacity:5}") int authCapacity,
        @org.springframework.beans.factory.annotation.Value("${app.ratelimit.admin.capacity:60}") int adminCapacity,
        @org.springframework.beans.factory.annotation.Value("${app.ratelimit.global.capacity:300}") int globalCapacity
    ) {
        this.authCapacity = authCapacity;
        this.adminCapacity = adminCapacity;
        this.globalCapacity = globalCapacity;
    }

    @Override
    protected void doFilterInternal(@jakarta.annotation.Nonnull HttpServletRequest request,
                                    @jakarta.annotation.Nonnull HttpServletResponse response,
                                    @jakarta.annotation.Nonnull FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (shouldSkip(path)) {
            chain.doFilter(request, response);
            return;
        }
        int capacity = capacityFor(path);
        String key = request.getRemoteAddr() + ":" + bucketName(path);
        Bucket bucket = buckets.computeIfAbsent(key, k -> new Bucket(capacity));
        if (!bucket.tryConsume()) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setHeader("Retry-After", "60");
            response.getWriter().write(
                "{\"code\":\"rate_limited\",\"detail\":\"Muitas requisições. Tente novamente em alguns segundos.\"}"
            );
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean shouldSkip(String path) {
        return path.startsWith("/docs")
            || path.startsWith("/openapi")
            || path.startsWith("/swagger-ui")
            || path.equals("/")
            || path.startsWith("/health")
            || path.contains("/public/tenants")  // páginas públicas de parceiros
            || path.contains("/feed.xml")
            || path.contains("/feed-meta.csv");
    }

    private String bucketName(String path) {
        if (path.startsWith("/auth")) return "auth";
        if (path.startsWith("/admin")) return "admin";
        return "global";
    }

    private int capacityFor(String path) {
        if (path.startsWith("/auth")) return authCapacity;
        if (path.startsWith("/admin")) return adminCapacity;
        return globalCapacity;
    }

    /**
     * Janela fixa de 60s. Simples, sem token bucket: conta incrementos
     * por minuto e reseta ao virar o segundo 0.
     */
    private static final class Bucket {
        private final int capacity;
        private final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis() / 60_000);
        private final AtomicInteger count = new AtomicInteger(0);

        Bucket(int capacity) { this.capacity = capacity; }

        synchronized boolean tryConsume() {
            long currentMinute = System.currentTimeMillis() / 60_000;
            long start = windowStart.get();
            if (currentMinute != start) {
                windowStart.set(currentMinute);
                count.set(0);
            }
            if (count.get() >= capacity) return false;
            count.incrementAndGet();
            return true;
        }
    }
}
