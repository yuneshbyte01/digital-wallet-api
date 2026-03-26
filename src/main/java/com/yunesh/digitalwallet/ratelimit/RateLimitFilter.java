package com.yunesh.digitalwallet.ratelimit;

import com.yunesh.digitalwallet.auth.JwtService;
import com.yunesh.digitalwallet.common.AppConstants;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RateLimitBucketService bucketService;
    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // login rate limit — per IP
        if (path.contains("/auth/login")) {
            String ip = request.getRemoteAddr();
            Bucket bucket = bucketService.getLoginBucket(ip);

            if (!bucket.tryConsume(1)) {
                response.setStatus(429);
                response.setHeader("Retry-After",
                        String.valueOf(
                                AppConstants.RateLimit.LOGIN_REFILL_SECONDS));
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"status\":429,\"error\":\"Too Many Requests\","
                                + "\"message\":\"Too many login attempts. "
                                + "Please try again later.\","
                                + "\"timestamp\":\"" + java.time.Instant.now() + "\"}");
                return;
            }
        }

        // transfer rate limit — per authenticated user
        if (path.contains("/transfers") && "POST".equals(request.getMethod())) {
            String header = request.getHeader("Authorization");

            if (header != null && header.startsWith("Bearer ")) {
                String token = header.substring(7);

                if (jwtService.validateToken(token)) {
                    String email = jwtService.extractEmail(token);
                    Bucket bucket = bucketService.getTransferBucket(email);

                    if (!bucket.tryConsume(1)) {
                        response.setStatus(429);
                        response.setHeader("Retry-After",
                                String.valueOf(
                                        AppConstants.RateLimit.TRANSFER_REFILL_SECONDS));
                        response.setContentType("application/json");
                        response.getWriter().write(
                                "{\"status\":429,\"error\":\"Too Many Requests\","
                                        + "\"message\":\"Transfer rate limit exceeded. "
                                        + "Maximum 5 transfers per minute.\","
                                        + "\"timestamp\":\"" + java.time.Instant.now()
                                        + "\"}");
                        return;
                    }
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}