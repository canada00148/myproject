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
        // 解析監聽端口
        String listenPortsStr = properties.getProperty("listen.ports", "8080");
        listenPorts = parsePortMappings(listenPortsStr);
        
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
        
        logger.info("配置加載完成: 監聽端口={}, 遠程服務器={}", listenPorts, remoteHost);
    }
    
    private List<Integer> parsePortMappings(String portMappings) {
        List<Integer> ports = new ArrayList<>();
        String[] parts = portMappings.split(",");
        
        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;
            
            // 檢查是否為端口範圍 (例如: 2000-2010)
            Matcher rangeMatcher = PORT_RANGE_PATTERN.matcher(part);
            if (rangeMatcher.matches()) {
                int start = Integer.parseInt(rangeMatcher.group(1));
                int end = Integer.parseInt(rangeMatcher.group(2));
                if (start > end) {
                    int temp = start;
                    start = end;
                    end = temp;
                }
                for (int port = start; port <= end; port++) {
                    if (port >= 1 && port <= 65535) {
                        ports.add(port);
                    }
                }
            } else {
                // 單個端口
                Matcher singleMatcher = PORT_SINGLE_PATTERN.matcher(part);
                if (singleMatcher.matches()) {
                    int port = Integer.parseInt(part);
                    if (port >= 1 && port <= 65535) {
                        ports.add(port);
                    }
                }
            }
        }
        
        // 去重並排序
        Set<Integer> uniquePorts = new LinkedHashSet<>(ports);
        List<Integer> result = new ArrayList<>(uniquePorts);
        Collections.sort(result);
        
        return result;
    }
    
    // Getters
    public List<Integer> getListenPorts() { return listenPorts; }
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