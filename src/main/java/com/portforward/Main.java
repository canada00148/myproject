package com.portforward;

import com.portforward.config.Config;
import com.portforward.server.PortForwardServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class Main {
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    
    public static void main(String[] args) {
        logger.info("啟動Java NIO端口轉發服務器...");
        
        try {
            // 加載配置
            Config config = new Config();
            logger.info("配置加載成功");
            
            // 創建並啟動服務器
            PortForwardServer server = new PortForwardServer(config);
            server.start();
            
            logger.info("端口轉發服務器啟動成功！");
            logger.info("監聽端口: {}", config.getListenPorts());
            logger.info("遠程端口: {}", config.getRemotePorts());
            logger.info("遠程服務器: {} (1:1端口映射)", config.getRemoteHost());
            logger.info("安全配置:");
            logger.info("  - 最大封包大小: {} bytes", config.getMaxPacketSize());
            logger.info("  - 最小封包大小: {} bytes", config.getMinPacketSize());
            logger.info("  - 每IP最大連接數: {}", config.getMaxConnectionsPerIp());
            logger.info("  - 連接速率限制: {}/{}ms", config.getConnectionRateLimit(), config.getRateLimitWindow());
            logger.info("  - TCP ping檢測: {}", config.isTcpPingDetection());
            
            // 添加關閉鉤子
            Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                @Override
                public void run() {
                    logger.info("收到關閉信號，正在停止服務器...");
                    server.stop();
                    logger.info("服務器已停止");
                }
            }));
            
            // 保持主線程運行
            while (server.isRunning()) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
        } catch (Exception e) {
            logger.error("啟動服務器失敗", e);
            System.exit(1);
        }
    }
}