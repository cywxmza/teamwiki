package com.teamwiki.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, Object> redisTemplate;

    private static final int MAX_REQUESTS_PER_MINUTE = 120;
    private static final int WRITE_LIMIT_PER_MINUTE = 60;

    public RateLimitFilter(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith("/ws/") || path.startsWith("/uploads/") || path.startsWith("/api/v1/auth/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String clientIp = getClientIp(request);
        String key = "rate_limit:" + clientIp;

        boolean isWrite = "POST".equalsIgnoreCase(request.getMethod())
                || "PUT".equalsIgnoreCase(request.getMethod())
                || "DELETE".equalsIgnoreCase(request.getMethod())
                || "PATCH".equalsIgnoreCase(request.getMethod());

        int limit = isWrite ? WRITE_LIMIT_PER_MINUTE : MAX_REQUESTS_PER_MINUTE;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count == null) count = 0L;

        if (count == 1) {
            redisTemplate.expire(key, 1, TimeUnit.MINUTES);
        }

        if (count > limit) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"status\":429,\"message\":\"请求过于频繁，请稍后重试\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/ws/") || path.startsWith("/uploads/")
                || path.startsWith("/api/v1/auth/") || path.startsWith("/api/v1/documents/shared/");
    }
}
