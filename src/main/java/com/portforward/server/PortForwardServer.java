package com.portforward.server;

import com.portforward.config.Config;
import com.portforward.handler.ConnectionHandler;
import com.portforward.protocol.ProxyProtocolParser;
import com.portforward.security.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class PortForwardServer {
    private static final Logger logger = LoggerFactory.getLogger(PortForwardServer.class);
    
    private final Config config;
    private final SecurityManager securityManager;
    private final ExecutorService executorService;
    private final Map<Integer, ServerSocketChannel> serverChannels = new HashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Selector selector;
    
    public PortForwardServer(Config config) {
        this.config = config;
        this.securityManager = new SecurityManager(
                config.getMaxConnectionsPerIp(),
                config.getConnectionRateLimit(),
                config.getRateLimitWindow(),
                config.getMaxPacketSize(),
                config.getMinPacketSize(),
                config.isTcpPingDetection(),
                config.getTcpPingTimeout(),
                config.getMaxIdleTime()
        );
        this.executorService = Executors.newCachedThreadPool();
    }
    
    /**
     * 啟動服務器
     */
    public void start() throws IOException {
        if (running.compareAndSet(false, true)) {
            logger.info("啟動端口轉發服務器...");
            
            // 創建選擇器
            selector = Selector.open();
            
            // 綁定所有端口
            List<Integer> ports = config.getLocalPorts();
            for (int port : ports) {
                bindPort(port);
            }
            
            logger.info("端口轉發服務器啟動完成，監聽端口: {}", ports);
            
            // 啟動事件循環
            startEventLoop();
        }
    }
    
    /**
     * 綁定端口
     */
    private void bindPort(int port) throws IOException {
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);
        serverChannel.socket().bind(new InetSocketAddress(port));
        serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        
        serverChannels.put(port, serverChannel);
        logger.info("綁定端口: {}", port);
    }
    
    /**
     * 啟動事件循環
     */
    private void startEventLoop() {
        Thread eventLoopThread = new Thread(() -> {
            try {
                while (running.get()) {
                    int readyChannels = selector.select(1000); // 1秒超時
                    if (readyChannels == 0) {
                        continue;
                    }
                    
                    for (SelectionKey key : selector.selectedKeys()) {
                        if (!key.isValid()) {
                            continue;
                        }
                        
                        if (key.isAcceptable()) {
                            handleAccept(key);
                        }
                    }
                    
                    selector.selectedKeys().clear();
                }
            } catch (IOException e) {
                if (running.get()) {
                    logger.error("事件循環異常", e);
                }
            }
        }, "PortForward-EventLoop");
        
        eventLoopThread.setDaemon(true);
        eventLoopThread.start();
    }
    
    /**
     * 處理接受連接事件
     */
    private void handleAccept(SelectionKey key) {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        
        try {
            SocketChannel clientChannel = serverChannel.accept();
            if (clientChannel == null) {
                return;
            }
            
            // 設置TCP選項
            clientChannel.socket().setTcpNoDelay(true);
            clientChannel.socket().setKeepAlive(true);
            clientChannel.socket().setSoTimeout(config.getConnectionTimeout());
            
            // 獲取客戶端地址
            InetSocketAddress clientAddress = (InetSocketAddress) clientChannel.getRemoteAddress();
            
            // 檢查連接限制
            if (!securityManager.isConnectionAllowed(clientAddress.getAddress())) {
                logger.warn("拒絕來自 {} 的連接（超過限制）", clientAddress.getAddress().getHostAddress());
                clientChannel.close();
                return;
            }
            
            // 解析Proxy Protocol
            ProxyProtocolParser.ProxyInfo proxyInfo = null;
            try {
                proxyInfo = ProxyProtocolParser.parseProxyProtocol(clientChannel);
                if (proxyInfo != null) {
                    logger.info("檢測到Proxy Protocol: {}", proxyInfo);
                }
            } catch (Exception e) {
                logger.warn("解析Proxy Protocol失敗", e);
                clientChannel.close();
                return;
            }
            
            // 創建到遠程服務器的連接
            SocketChannel remoteChannel = createRemoteConnection();
            if (remoteChannel == null) {
                logger.error("無法連接到遠程服務器");
                clientChannel.close();
                return;
            }
            
            // 跳過Proxy Protocol頭部
            if (proxyInfo != null) {
                ProxyProtocolParser.skipProxyProtocolHeader(clientChannel, proxyInfo);
            }
            
            // 創建連接處理器
            ConnectionHandler handler = new ConnectionHandler(
                    clientChannel, remoteChannel, securityManager, 
                    clientAddress.getAddress(), proxyInfo
            );
            
            // 在線程池中處理連接
            executorService.submit(() -> {
                try {
                    handler.handle();
                } catch (Exception e) {
                    logger.error("處理連接失敗", e);
                }
            });
            
        } catch (IOException e) {
            logger.error("接受連接失敗", e);
        }
    }
    
    /**
     * 創建到遠程服務器的連接
     */
    private SocketChannel createRemoteConnection() {
        try {
            SocketChannel remoteChannel = SocketChannel.open();
            remoteChannel.configureBlocking(true);
            remoteChannel.socket().setTcpNoDelay(true);
            remoteChannel.socket().setKeepAlive(true);
            remoteChannel.socket().setSoTimeout(config.getReadTimeout());
            remoteChannel.socket().setSendBufferSize(config.getMaxPacketSize());
            remoteChannel.socket().setReceiveBufferSize(config.getMaxPacketSize());
            
            // 連接到遠程服務器
            InetSocketAddress remoteAddress = new InetSocketAddress(config.getRemoteHost(), config.getRemotePort());
            remoteChannel.connect(remoteAddress);
            
            return remoteChannel;
            
        } catch (IOException e) {
            logger.error("連接到遠程服務器失敗: {}:{}", config.getRemoteHost(), config.getRemotePort(), e);
            return null;
        }
    }
    
    /**
     * 停止服務器
     */
    public void stop() {
        if (running.compareAndSet(true, false)) {
            logger.info("停止端口轉發服務器...");
            
            try {
                // 關閉所有服務器通道
                for (ServerSocketChannel channel : serverChannels.values()) {
                    if (channel != null && channel.isOpen()) {
                        channel.close();
                    }
                }
                serverChannels.clear();
                
                // 關閉選擇器
                if (selector != null && selector.isOpen()) {
                    selector.close();
                }
                
                // 關閉線程池
                executorService.shutdown();
                
                // 關閉安全管理器
                securityManager.shutdown();
                
                logger.info("端口轉發服務器已停止");
                
            } catch (IOException e) {
                logger.error("停止服務器時發生錯誤", e);
            }
        }
    }
    
    /**
     * 檢查服務器是否正在運行
     */
    public boolean isRunning() {
        return running.get();
    }
    
    /**
     * 獲取監聽的端口列表
     */
    public List<Integer> getListeningPorts() {
        return config.getLocalPorts();
    }
}