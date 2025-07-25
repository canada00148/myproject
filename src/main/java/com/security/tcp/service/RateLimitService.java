package com.security.tcp.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.util.concurrent.RateLimiter;
import com.security.tcp.config.ServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * TCP層級的速率限制服務
 */
public class RateLimitService {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    private static final RateLimitService INSTANCE = new RateLimitService();
    
    private final ServerConfig config = ServerConfig.getInstance();
    private final RateLimiter globalRateLimiter;
    private final ConcurrentHashMap<String, RateLimiter> ipRateLimiters = new ConcurrentHashMap<>();
    private final Cache<String, AtomicInteger> requestCountCache;
    
    private RateLimitService() {
        // 初始化全局速率限制器
        globalRateLimiter = RateLimiter.create(config.getGlobalRateLimit());
        
        // 初始化請求計數緩存
        requestCountCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.MINUTES)
                .maximumSize(10000)
                .build();
    }
    
    public static RateLimitService getInstance() {
        return INSTANCE;
    }
    
    /**
     * 檢查是否允許請求（全局限流）
     */
    public boolean allowGlobalRequest() {
        boolean allowed = globalRateLimiter.tryAcquire();
        if (!allowed) {
            logger.warn("Global rate limit exceeded");
        }
        return allowed;
    }
    
    /**
     * 檢查是否允許來自特定IP的請求
     */
    public boolean allowRequestFromIp(String clientIp) {
        // 獲取或創建IP專用的限流器
        RateLimiter ipLimiter = ipRateLimiters.computeIfAbsent(clientIp, 
            ip -> RateLimiter.create(config.getPerIpRateLimit()));
        
        boolean allowed = ipLimiter.tryAcquire();
        if (!allowed) {
            logger.warn("IP rate limit exceeded for: {}", clientIp);
        }
        
        return allowed;
    }
    
    /**
     * 檢查基於時間窗口的請求數限制
     */
    public boolean checkWindowLimit(String key, int maxRequests, int windowSeconds) {
        String cacheKey = key + ":" + (System.currentTimeMillis() / (windowSeconds * 1000));
        AtomicInteger count = requestCountCache.get(cacheKey, k -> new AtomicInteger(0));
        
        int currentCount = count.incrementAndGet();
        if (currentCount > maxRequests) {
            logger.warn("Window rate limit exceeded for key: {}, count: {}/{}", 
                key, currentCount, maxRequests);
            return false;
        }
        
        return true;
    }
    
    /**
     * 獲取IP的當前速率
     */
    public double getCurrentRate(String clientIp) {
        RateLimiter limiter = ipRateLimiters.get(clientIp);
        return limiter != null ? limiter.getRate() : 0.0;
    }
    
    /**
     * 調整特定IP的速率限制
     */
    public void adjustIpRateLimit(String clientIp, double newRate) {
        RateLimiter limiter = ipRateLimiters.get(clientIp);
        if (limiter != null) {
            limiter.setRate(newRate);
            logger.info("Adjusted rate limit for IP {} to {} requests/second", clientIp, newRate);
        }
    }
    
    /**
     * 清理過期的限流器
     */
    public void cleanup() {
        // 如果IP限流器太多，清理一些
        if (ipRateLimiters.size() > 10000) {
            ipRateLimiters.clear();
            logger.info("Cleared IP rate limiters due to size threshold");
        }
    }
}