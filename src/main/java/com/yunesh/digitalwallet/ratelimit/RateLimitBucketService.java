package com.yunesh.digitalwallet.ratelimit;

import com.yunesh.digitalwallet.common.AppConstants;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitBucketService {

    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    public Bucket getTransferBucket(String userId) {
        return buckets.computeIfAbsent("transfer:" + userId,
                k -> newTransferBucket());
    }

    public Bucket getLoginBucket(String ipAddress) {
        return buckets.computeIfAbsent("login:" + ipAddress,
                k -> newLoginBucket());
    }

    private Bucket newTransferBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(AppConstants.RateLimit.TRANSFER_CAPACITY)
                        .refillIntervally(
                                AppConstants.RateLimit.TRANSFER_CAPACITY,
                                Duration.ofSeconds(
                                        AppConstants.RateLimit.TRANSFER_REFILL_SECONDS))
                        .build())
                .build();
    }

    private Bucket newLoginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(AppConstants.RateLimit.LOGIN_CAPACITY)
                        .refillIntervally(
                                AppConstants.RateLimit.LOGIN_CAPACITY,
                                Duration.ofSeconds(
                                        AppConstants.RateLimit.LOGIN_REFILL_SECONDS))
                        .build())
                .build();
    }
}