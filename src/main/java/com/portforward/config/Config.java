package com.portforward.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Config {
    private static final Logger logger = LoggerFactory.getLogger(Config.class);
    private static final Pattern PORT_RANGE_PATTERN = Pattern.compile("(\\d+)-(\\d+)");
    private static final Pattern PORT_SINGLE_PATTERN = Pattern.compile("(\\d+)");
    
    private Properties properties;
    private List<Integer> listenPorts;
    private List<Integer> remotePorts;
    private String remoteHost;
    private int maxPacketSize;
    private int minPacketSize;
    private int connectionTimeout;
    private int readTimeout;
    private int writeTimeout;
    private int maxConnectionsPerIp;
    private int connectionRateLimit;
    private int rateLimitWindow;
    private boolean tcpPingDetection;
    private int tcpPingTimeout;
    private int maxIdleTime;
    
    public Config() {
        loadConfig();
    }
    
    private void loadConfig() {
        properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            if (input == null) {
                // 嘗試從當前目錄加載
                try (InputStream fileInput = new java.io.FileInputStream("config.properties")) {
                    properties.load(fileInput);
                }
            } else {
                properties.load(input);
            }
            parseConfig();
        } catch (IOException e) {
            logger.error("無法加載配置文件", e);
            throw new RuntimeException("配置加載失敗", e);
        }
    }
    
    private void parseConfig() {
        // 解析端口映射
        String portMappingsStr = properties.getProperty("port.mappings", "8080=8080");
        parsePortMappings(portMappingsStr);
        
        // 解析遠程服務器配置
        remoteHost = properties.getProperty("remote.host", "127.0.0.1");
        
        // 解析安全配置
        maxPacketSize = Integer.parseInt(properties.getProperty("security.max_packet_size", "8192"));
        minPacketSize = Integer.parseInt(properties.getProperty("security.min_packet_size", "1"));
        connectionTimeout = Integer.parseInt(properties.getProperty("security.connection_timeout", "30000"));
        readTimeout = Integer.parseInt(properties.getProperty("security.read_timeout", "10000"));
        writeTimeout = Integer.parseInt(properties.getProperty("security.write_timeout", "10000"));
        
        // 解析抗CC攻擊配置
        maxConnectionsPerIp = Integer.parseInt(properties.getProperty("security.max_connections_per_ip", "100"));
        connectionRateLimit = Integer.parseInt(properties.getProperty("security.connection_rate_limit", "10"));
        rateLimitWindow = Integer.parseInt(properties.getProperty("security.rate_limit_window", "60000"));
        
        // 解析TCP層抗攻擊配置
        tcpPingDetection = Boolean.parseBoolean(properties.getProperty("security.tcp_ping_detection", "true"));
        tcpPingTimeout = Integer.parseInt(properties.getProperty("security.tcp_ping_timeout", "5000"));
        maxIdleTime = Integer.parseInt(properties.getProperty("security.max_idle_time", "300000"));
        
        logger.info("配置加載完成: 監聽端口={}, 遠程端口={}, 遠程服務器={}", listenPorts, remotePorts, remoteHost);
    }
    
    private void parsePortMappings(String portMappings) {
        listenPorts = new ArrayList<>();
        remotePorts = new ArrayList<>();
        
        String[] mappings = portMappings.split(",");
        
        for (String mapping : mappings) {
            mapping = mapping.trim();
            if (mapping.isEmpty()) continue;
            
            // 解析 監聽端口=遠程端口 格式
            String[] parts = mapping.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException("端口映射格式錯誤: " + mapping + 
                    "，正確格式: 監聽端口=遠程端口");
            }
            
            String listenPortStr = parts[0].trim();
            String remotePortStr = parts[1].trim();
            
            // 解析監聽端口
            int listenPort = parseSinglePort(listenPortStr);
            // 解析遠程端口
            int remotePort = parseSinglePort(remotePortStr);
            
            // 檢查端口範圍
            if (listenPort < 1 || listenPort > 65535) {
                throw new IllegalArgumentException("監聽端口超出範圍: " + listenPort);
            }
            if (remotePort < 1 || remotePort > 65535) {
                throw new IllegalArgumentException("遠程端口超出範圍: " + remotePort);
            }
            
            // 按照配置文件順序添加，不進行任何排序
            listenPorts.add(listenPort);
            remotePorts.add(remotePort);
        }
        
        // 只檢查端口數量一致性
        if (listenPorts.size() != remotePorts.size()) {
            throw new IllegalArgumentException("監聽端口和遠程端口數量不一致: " + 
                "監聽端口=" + listenPorts.size() + ", 遠程端口=" + remotePorts.size());
        }
        
        logger.info("端口映射解析完成: {} 個映射", listenPorts.size());
        for (int i = 0; i < listenPorts.size(); i++) {
            logger.info("  監聽端口 {} -> 遠程端口 {}", listenPorts.get(i), remotePorts.get(i));
        }
    }
    
    private int parseSinglePort(String portStr) {
        try {
            return Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("端口格式錯誤: " + portStr);
        }
    }
    
    // Getters
    public List<Integer> getListenPorts() { return listenPorts; }
    public List<Integer> getRemotePorts() { return remotePorts; }
    public String getRemoteHost() { return remoteHost; }
    public int getMaxPacketSize() { return maxPacketSize; }
    public int getMinPacketSize() { return minPacketSize; }
    public int getConnectionTimeout() { return connectionTimeout; }
    public int getReadTimeout() { return readTimeout; }
    public int getWriteTimeout() { return writeTimeout; }
    public int getMaxConnectionsPerIp() { return maxConnectionsPerIp; }
    public int getConnectionRateLimit() { return connectionRateLimit; }
    public int getRateLimitWindow() { return rateLimitWindow; }
    public boolean isTcpPingDetection() { return tcpPingDetection; }
    public int getTcpPingTimeout() { return tcpPingTimeout; }
    public int getMaxIdleTime() { return maxIdleTime; }
}