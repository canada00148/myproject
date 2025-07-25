package com.security.tcp.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.security.tcp.config.ServerConfig;
import com.security.tcp.model.ConnectionInfo;
import io.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 連接管理服務，負責管理和監控所有TCP連接
 */
public class ConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionManager.class);
    private static final ConnectionManager INSTANCE = new ConnectionManager();
    
    private final Map<String, ConnectionInfo> activeConnections = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> ipConnectionCount = new ConcurrentHashMap<>();
    private final Cache<String, AtomicInteger> connectionRateCache;
    private final ServerConfig config = ServerConfig.getInstance();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private ConnectionManager() {
        // 初始化連接速率緩存，用於檢測短時間內的大量連接
        connectionRateCache = Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.SECONDS)
                .build();
        
        // 定期清理空閒連接
        scheduler.scheduleAtFixedRate(this::cleanupIdleConnections, 30, 30, TimeUnit.SECONDS);
    }
    
    public static ConnectionManager getInstance() {
        return INSTANCE;
    }
    
    /**
     * 註冊新連接
     * @return true如果允許連接，false如果應該拒絕
     */
    public boolean registerConnection(Channel channel) {
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        String clientIp = remoteAddress.getAddress().getHostAddress();
        String channelId = channel.id().asLongText();
        
        // 檢查每秒連接數限制
        if (!checkConnectionRate(clientIp)) {
            logger.warn("Connection rate limit exceeded for IP: {}", clientIp);
            return false;
        }
        
        // 檢查單IP連接數限制
        AtomicInteger count = ipConnectionCount.computeIfAbsent(clientIp, k -> new AtomicInteger(0));
        if (count.get() >= config.getMaxConnectionsPerIp()) {
            logger.warn("Max connections per IP exceeded for: {} (current: {})", clientIp, count.get());
            return false;
        }
        
        // 註冊連接
        ConnectionInfo connInfo = new ConnectionInfo(remoteAddress);
        activeConnections.put(channelId, connInfo);
        count.incrementAndGet();
        
        logger.info("New connection registered: {} from {}", channelId, clientIp);
        return true;
    }
    
    /**
     * 註銷連接
     */
    public void unregisterConnection(Channel channel) {
        String channelId = channel.id().asLongText();
        ConnectionInfo connInfo = activeConnections.remove(channelId);
        
        if (connInfo != null) {
            String clientIp = connInfo.getClientIp();
            AtomicInteger count = ipConnectionCount.get(clientIp);
            if (count != null) {
                int remaining = count.decrementAndGet();
                if (remaining <= 0) {
                    ipConnectionCount.remove(clientIp);
                }
            }
            
            logger.info("Connection unregistered: {} from {} (duration: {}ms, received: {} bytes, sent: {} bytes)",
                    channelId, clientIp, connInfo.getConnectionDuration(), 
                    connInfo.getBytesReceived(), connInfo.getBytesSent());
        }
    }
    
    /**
     * 獲取連接信息
     */
    public ConnectionInfo getConnectionInfo(Channel channel) {
        return activeConnections.get(channel.id().asLongText());
    }
    
    /**
     * 檢查連接速率
     */
    private boolean checkConnectionRate(String clientIp) {
        AtomicInteger rate = connectionRateCache.get(clientIp, k -> new AtomicInteger(0));
        int currentRate = rate.incrementAndGet();
        
        if (currentRate > config.getMaxConnectionsPerSecond()) {
            return false;
        }
        
        return true;
    }
    
    /**
     * 清理空閒連接
     */
    private void cleanupIdleConnections() {
        long idleTimeout = config.getReadTimeoutSeconds() * 1000L;
        int cleaned = 0;
        
        for (Map.Entry<String, ConnectionInfo> entry : activeConnections.entrySet()) {
            ConnectionInfo connInfo = entry.getValue();
            if (connInfo.isIdle(idleTimeout)) {
                activeConnections.remove(entry.getKey());
                cleaned++;
            }
        }
        
        if (cleaned > 0) {
            logger.info("Cleaned up {} idle connections", cleaned);
        }
    }
    
    /**
     * 獲取當前活躍連接數
     */
    public int getActiveConnectionCount() {
        return activeConnections.size();
    }
    
    /**
     * 獲取指定IP的連接數
     */
    public int getConnectionCountByIp(String ip) {
        AtomicInteger count = ipConnectionCount.get(ip);
        return count != null ? count.get() : 0;
    }
    
    /**
     * 關閉服務
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}