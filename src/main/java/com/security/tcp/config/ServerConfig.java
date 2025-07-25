package com.security.tcp.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * 伺服器配置類
 */
public class ServerConfig {
    private static final ServerConfig INSTANCE = new ServerConfig();
    
    // CC防護配置
    private boolean ccProtectionEnabled = true;
    private int maxConnectionsPerIp = 10;
    private int maxConnectionsPerSecond = 5;
    private int connectionBanDuration = 300; // 秒
    
    // 封包驗證配置
    private boolean packetValidationEnabled = true;
    private int maxPacketSize = 65536; // 64KB
    private int minPacketSize = 1;
    private boolean dropInvalidPackets = true;
    
    // 性能配置
    private int workerThreads = Runtime.getRuntime().availableProcessors() * 2;
    private int readTimeoutSeconds = 30;
    private int writeTimeoutSeconds = 30;
    
    // 限流配置
    private double globalRateLimit = 1000.0; // 每秒請求數
    private double perIpRateLimit = 100.0;   // 每個IP每秒請求數
    
    private ServerConfig() {
        loadConfig();
    }
    
    public static ServerConfig getInstance() {
        return INSTANCE;
    }
    
    private void loadConfig() {
        Properties props = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("server.properties")) {
            if (input != null) {
                props.load(input);
                
                // 載入CC防護配置
                ccProtectionEnabled = Boolean.parseBoolean(
                    props.getProperty("cc.protection.enabled", "true"));
                maxConnectionsPerIp = Integer.parseInt(
                    props.getProperty("cc.max.connections.per.ip", "10"));
                maxConnectionsPerSecond = Integer.parseInt(
                    props.getProperty("cc.max.connections.per.second", "5"));
                connectionBanDuration = Integer.parseInt(
                    props.getProperty("cc.ban.duration.seconds", "300"));
                
                // 載入封包驗證配置
                packetValidationEnabled = Boolean.parseBoolean(
                    props.getProperty("packet.validation.enabled", "true"));
                maxPacketSize = Integer.parseInt(
                    props.getProperty("packet.max.size", "65536"));
                minPacketSize = Integer.parseInt(
                    props.getProperty("packet.min.size", "1"));
                dropInvalidPackets = Boolean.parseBoolean(
                    props.getProperty("packet.drop.invalid", "true"));
                
                // 載入性能配置
                workerThreads = Integer.parseInt(
                    props.getProperty("server.worker.threads", 
                        String.valueOf(Runtime.getRuntime().availableProcessors() * 2)));
                readTimeoutSeconds = Integer.parseInt(
                    props.getProperty("server.read.timeout.seconds", "30"));
                writeTimeoutSeconds = Integer.parseInt(
                    props.getProperty("server.write.timeout.seconds", "30"));
                
                // 載入限流配置
                globalRateLimit = Double.parseDouble(
                    props.getProperty("ratelimit.global.per.second", "1000.0"));
                perIpRateLimit = Double.parseDouble(
                    props.getProperty("ratelimit.per.ip.per.second", "100.0"));
            }
        } catch (IOException e) {
            // 使用默認配置
        }
    }
    
    // Getters
    public boolean isCcProtectionEnabled() {
        return ccProtectionEnabled;
    }
    
    public int getMaxConnectionsPerIp() {
        return maxConnectionsPerIp;
    }
    
    public int getMaxConnectionsPerSecond() {
        return maxConnectionsPerSecond;
    }
    
    public int getConnectionBanDuration() {
        return connectionBanDuration;
    }
    
    public boolean isPacketValidationEnabled() {
        return packetValidationEnabled;
    }
    
    public int getMaxPacketSize() {
        return maxPacketSize;
    }
    
    public int getMinPacketSize() {
        return minPacketSize;
    }
    
    public boolean isDropInvalidPackets() {
        return dropInvalidPackets;
    }
    
    public int getWorkerThreads() {
        return workerThreads;
    }
    
    public int getReadTimeoutSeconds() {
        return readTimeoutSeconds;
    }
    
    public int getWriteTimeoutSeconds() {
        return writeTimeoutSeconds;
    }
    
    public double getGlobalRateLimit() {
        return globalRateLimit;
    }
    
    public double getPerIpRateLimit() {
        return perIpRateLimit;
    }
}