package com.portforward.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class SecurityManager {
    private static final Logger logger = LoggerFactory.getLogger(SecurityManager.class);
    
    // 連接限制配置
    private final int maxConnectionsPerIp;
    private final int connectionRateLimit;
    private final int rateLimitWindow;
    
    // 封包大小限制
    private final int maxPacketSize;
    private final int minPacketSize;
    
    // TCP ping檢測配置
    private final boolean tcpPingDetection;
    private final int tcpPingTimeout;
    private final int maxIdleTime;
    
    // 連接計數器
    private final Map<String, AtomicInteger> connectionCounters = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    
    // 清理任務
    private final ScheduledExecutorService cleanupExecutor = Executors.newSingleThreadScheduledExecutor();
    
    public SecurityManager(int maxConnectionsPerIp, int connectionRateLimit, int rateLimitWindow,
                          int maxPacketSize, int minPacketSize, boolean tcpPingDetection, 
                          int tcpPingTimeout, int maxIdleTime) {
        this.maxConnectionsPerIp = maxConnectionsPerIp;
        this.connectionRateLimit = connectionRateLimit;
        this.rateLimitWindow = rateLimitWindow;
        this.maxPacketSize = maxPacketSize;
        this.minPacketSize = minPacketSize;
        this.tcpPingDetection = tcpPingDetection;
        this.tcpPingTimeout = tcpPingTimeout;
        this.maxIdleTime = maxIdleTime;
        
        // 啟動清理任務
        startCleanupTask();
    }
    
    /**
     * 檢查連接是否允許
     */
    public boolean isConnectionAllowed(InetAddress clientAddress) {
        String clientIp = clientAddress.getHostAddress();
        
        // 檢查連接數量限制
        AtomicInteger counter = connectionCounters.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        if (counter.get() >= maxConnectionsPerIp) {
            logger.warn("IP {} 連接數超過限制: {}", clientIp, counter.get());
            return false;
        }
        
        // 檢查連接速率限制
        RateLimiter rateLimiter = rateLimiters.computeIfAbsent(clientIp, k -> new RateLimiter(connectionRateLimit, rateLimitWindow));
        if (!rateLimiter.tryAcquire()) {
            logger.warn("IP {} 連接速率超過限制", clientIp);
            return false;
        }
        
        // 增加連接計數
        counter.incrementAndGet();
        logger.debug("IP {} 連接計數: {}", clientIp, counter.get());
        
        return true;
    }
    
    /**
     * 釋放連接
     */
    public void releaseConnection(InetAddress clientAddress) {
        String clientIp = clientAddress.getHostAddress();
        AtomicInteger counter = connectionCounters.get(clientIp);
        if (counter != null) {
            int currentCount = counter.decrementAndGet();
            if (currentCount <= 0) {
                connectionCounters.remove(clientIp);
                rateLimiters.remove(clientIp);
            }
            logger.debug("IP {} 釋放連接，當前計數: {}", clientIp, currentCount);
        }
    }
    
    /**
     * 檢查封包大小是否合法
     */
    public boolean isPacketSizeValid(int packetSize) {
        if (packetSize < minPacketSize) {
            logger.warn("封包大小過小: {} < {}", packetSize, minPacketSize);
            return false;
        }
        if (packetSize > maxPacketSize) {
            logger.warn("封包大小過大: {} > {}", packetSize, maxPacketSize);
            return false;
        }
        return true;
    }
    
    /**
     * 檢測TCP ping攻擊
     */
    public boolean isTcpPingAttack(long connectionTime, long lastActivityTime) {
        if (!tcpPingDetection) {
            return false;
        }
        
        long currentTime = System.currentTimeMillis();
        long idleTime = currentTime - lastActivityTime;
        
        // 檢查連接時間是否過短（可能是ping攻擊）
        long connectionDuration = currentTime - connectionTime;
        if (connectionDuration < tcpPingTimeout) {
            logger.warn("檢測到可能的TCP ping攻擊，連接時間: {}ms", connectionDuration);
            return true;
        }
        
        // 檢查空閒時間是否過長
        if (idleTime > maxIdleTime) {
            logger.warn("連接空閒時間過長: {}ms", idleTime);
            return true;
        }
        
        return false;
    }
    
    /**
     * 啟動清理任務
     */
    private void startCleanupTask() {
        cleanupExecutor.scheduleAtFixedRate(() -> {
            try {
                cleanupExpiredEntries();
            } catch (Exception e) {
                logger.error("清理任務執行失敗", e);
            }
        }, 1, 1, TimeUnit.MINUTES);
    }
    
    /**
     * 清理過期的條目
     */
    private void cleanupExpiredEntries() {
        long currentTime = System.currentTimeMillis();
        
        // 清理速率限制器
        rateLimiters.entrySet().removeIf(entry -> {
            RateLimiter limiter = entry.getValue();
            if (currentTime - limiter.getLastAccessTime() > rateLimitWindow) {
                logger.debug("清理過期的速率限制器: {}", entry.getKey());
                return true;
            }
            return false;
        });
        
        // 清理連接計數器（如果計數為0且長時間未使用）
        connectionCounters.entrySet().removeIf(entry -> {
            AtomicInteger counter = entry.getValue();
            if (counter.get() == 0) {
                logger.debug("清理空的連接計數器: {}", entry.getKey());
                return true;
            }
            return false;
        });
    }
    
    /**
     * 關閉安全管理器
     */
    public void shutdown() {
        cleanupExecutor.shutdown();
        try {
            if (!cleanupExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                cleanupExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            cleanupExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
    
    /**
     * 速率限制器
     */
    private static class RateLimiter {
        private final int maxRequests;
        private final long windowMs;
        private final AtomicInteger requestCount = new AtomicInteger(0);
        private volatile long windowStart;
        private volatile long lastAccessTime;
        
        public RateLimiter(int maxRequests, long windowMs) {
            this.maxRequests = maxRequests;
            this.windowMs = windowMs;
            this.windowStart = System.currentTimeMillis();
            this.lastAccessTime = System.currentTimeMillis();
        }
        
        public boolean tryAcquire() {
            long currentTime = System.currentTimeMillis();
            lastAccessTime = currentTime;
            
            // 檢查是否需要重置窗口
            if (currentTime - windowStart >= windowMs) {
                requestCount.set(0);
                windowStart = currentTime;
            }
            
            // 檢查是否超過限制
            return requestCount.incrementAndGet() <= maxRequests;
        }
        
        public long getLastAccessTime() {
            return lastAccessTime;
        }
    }
}