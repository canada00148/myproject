package com.security.antiattack.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 速率限制服務，提供多種限流策略
 */
@Slf4j
@Service
public class RateLimitService {
    
    @Autowired(required = false)
    private RedisTemplate<String, String> redisTemplate;
    
    // 本地緩存，用於單機限流
    private final Cache<String, AtomicInteger> requestCountCache = Caffeine.newBuilder()
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .maximumSize(10000)
            .build();
    
    // 令牌桶限流器
    private final ConcurrentHashMap<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    
    /**
     * 檢查是否允許請求
     * @param key 限流鍵
     * @param maxRequests 最大請求數
     * @param timeWindow 時間窗口（秒）
     * @return 是否允許請求
     */
    public boolean allowRequest(String key, int maxRequests, int timeWindow) {
        if (redisTemplate != null) {
            return allowRequestWithRedis(key, maxRequests, timeWindow);
        } else {
            return allowRequestLocal(key, maxRequests, timeWindow);
        }
    }
    
    /**
     * 使用Redis進行分佈式限流
     */
    private boolean allowRequestWithRedis(String key, int maxRequests, int timeWindow) {
        try {
            String redisKey = "rate_limit:" + key;
            Long count = redisTemplate.opsForValue().increment(redisKey);
            
            if (count == 1) {
                redisTemplate.expire(redisKey, timeWindow, TimeUnit.SECONDS);
            }
            
            if (count > maxRequests) {
                log.warn("Rate limit exceeded for key: {}, count: {}/{}", key, count, maxRequests);
                return false;
            }
            
            return true;
        } catch (Exception e) {
            log.error("Redis rate limit error: ", e);
            // Redis故障時降級為本地限流
            return allowRequestLocal(key, maxRequests, timeWindow);
        }
    }
    
    /**
     * 本地限流實現
     */
    private boolean allowRequestLocal(String key, int maxRequests, int timeWindow) {
        String cacheKey = key + ":" + (System.currentTimeMillis() / (timeWindow * 1000));
        AtomicInteger count = requestCountCache.get(cacheKey, k -> new AtomicInteger(0));
        
        int currentCount = count.incrementAndGet();
        if (currentCount > maxRequests) {
            log.warn("Local rate limit exceeded for key: {}, count: {}/{}", key, currentCount, maxRequests);
            return false;
        }
        
        return true;
    }
    
    /**
     * 使用令牌桶算法進行限流
     * @param key 限流鍵
     * @param permitsPerSecond 每秒許可數
     * @return 是否獲取到許可
     */
    public boolean tryAcquireWithTokenBucket(String key, double permitsPerSecond) {
        RateLimiter rateLimiter = rateLimiters.computeIfAbsent(key, 
            k -> RateLimiter.create(permitsPerSecond));
        
        boolean acquired = rateLimiter.tryAcquire();
        if (!acquired) {
            log.warn("Token bucket rate limit exceeded for key: {}", key);
        }
        
        return acquired;
    }
    
    /**
     * 清理過期的限流器
     */
    public void cleanupRateLimiters() {
        // 可以定期執行清理，避免內存洩漏
        if (rateLimiters.size() > 10000) {
            rateLimiters.clear();
            log.info("Cleared rate limiters due to size threshold");
        }
    }
}