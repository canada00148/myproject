package com.security.tcp.model;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * TCP連接信息
 */
public class ConnectionInfo {
    private final String clientIp;
    private final int clientPort;
    private final long connectTime;
    private final AtomicLong bytesReceived;
    private final AtomicLong bytesSent;
    private final AtomicInteger packetCount;
    private volatile long lastActivityTime;
    private volatile boolean suspicious;
    private volatile String suspiciousReason;
    
    public ConnectionInfo(InetSocketAddress remoteAddress) {
        this.clientIp = remoteAddress.getAddress().getHostAddress();
        this.clientPort = remoteAddress.getPort();
        this.connectTime = System.currentTimeMillis();
        this.lastActivityTime = connectTime;
        this.bytesReceived = new AtomicLong(0);
        this.bytesSent = new AtomicLong(0);
        this.packetCount = new AtomicInteger(0);
        this.suspicious = false;
    }
    
    public void updateActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }
    
    public void addBytesReceived(long bytes) {
        this.bytesReceived.addAndGet(bytes);
        this.packetCount.incrementAndGet();
        updateActivity();
    }
    
    public void addBytesSent(long bytes) {
        this.bytesSent.addAndGet(bytes);
        updateActivity();
    }
    
    public void markSuspicious(String reason) {
        this.suspicious = true;
        this.suspiciousReason = reason;
    }
    
    public boolean isIdle(long idleTimeoutMillis) {
        return System.currentTimeMillis() - lastActivityTime > idleTimeoutMillis;
    }
    
    public long getConnectionDuration() {
        return System.currentTimeMillis() - connectTime;
    }
    
    // Getters
    public String getClientIp() {
        return clientIp;
    }
    
    public int getClientPort() {
        return clientPort;
    }
    
    public long getConnectTime() {
        return connectTime;
    }
    
    public long getBytesReceived() {
        return bytesReceived.get();
    }
    
    public long getBytesSent() {
        return bytesSent.get();
    }
    
    public int getPacketCount() {
        return packetCount.get();
    }
    
    public long getLastActivityTime() {
        return lastActivityTime;
    }
    
    public boolean isSuspicious() {
        return suspicious;
    }
    
    public String getSuspiciousReason() {
        return suspiciousReason;
    }
    
    @Override
    public String toString() {
        return String.format("ConnectionInfo{ip=%s, port=%d, duration=%dms, received=%d, sent=%d, packets=%d, suspicious=%s}",
                clientIp, clientPort, getConnectionDuration(), getBytesReceived(), 
                getBytesSent(), getPacketCount(), suspicious);
    }
}