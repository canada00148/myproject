package com.portforward.handler;

import com.portforward.protocol.ProxyProtocolParser;
import com.portforward.security.SecurityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicLong;

public class ConnectionHandler {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
    private static final AtomicLong connectionIdGenerator = new AtomicLong(0);
    
    private final long connectionId;
    private final SocketChannel clientChannel;
    private final SocketChannel remoteChannel;
    private final SecurityManager securityManager;
    private final InetAddress clientAddress;
    private final ProxyProtocolParser.ProxyInfo proxyInfo;
    
    private final long connectionTime;
    private volatile long lastActivityTime;
    private volatile boolean closed = false;
    
    // 緩衝區
    private final ByteBuffer clientBuffer = ByteBuffer.allocate(8192);
    private final ByteBuffer remoteBuffer = ByteBuffer.allocate(8192);
    
    public ConnectionHandler(SocketChannel clientChannel, SocketChannel remoteChannel, 
                           SecurityManager securityManager, InetAddress clientAddress,
                           ProxyProtocolParser.ProxyInfo proxyInfo) {
        this.connectionId = connectionIdGenerator.incrementAndGet();
        this.clientChannel = clientChannel;
        this.remoteChannel = remoteChannel;
        this.securityManager = securityManager;
        this.clientAddress = clientAddress;
        this.proxyInfo = proxyInfo;
        this.connectionTime = System.currentTimeMillis();
        this.lastActivityTime = connectionTime;
        
        logger.info("創建連接處理器 [{}]: {} -> {}:{}", 
                connectionId, 
                proxyInfo != null ? proxyInfo.getSourceAddress().getHostAddress() : clientAddress.getHostAddress(),
                remoteChannel.socket().getInetAddress().getHostAddress(),
                remoteChannel.socket().getPort());
    }
    
    /**
     * 處理連接
     */
    public void handle() {
        try {
            // 設置非阻塞模式
            clientChannel.configureBlocking(false);
            remoteChannel.configureBlocking(false);
            
            // 註冊到選擇器
            Selector selector = Selector.open();
            clientChannel.register(selector, SelectionKey.OP_READ, this);
            remoteChannel.register(selector, SelectionKey.OP_READ, this);
            
            // 事件循環
            while (!closed && selector.isOpen()) {
                int readyChannels = selector.select(1000); // 1秒超時
                if (readyChannels == 0) {
                    // 檢查連接是否超時
                    checkTimeout();
                    continue;
                }
                
                for (SelectionKey key : selector.selectedKeys()) {
                    if (!key.isValid()) {
                        continue;
                    }
                    
                    try {
                        if (key.isReadable()) {
                            handleRead(key);
                        }
                    } catch (Exception e) {
                        logger.error("處理讀取事件失敗 [{}]", connectionId, e);
                        close();
                        break;
                    }
                }
                
                selector.selectedKeys().clear();
            }
            
        } catch (Exception e) {
            logger.error("連接處理失敗 [{}]", connectionId, e);
        } finally {
            close();
        }
    }
    
    /**
     * 處理讀取事件
     */
    private void handleRead(SelectionKey key) throws IOException {
        SocketChannel sourceChannel = (SocketChannel) key.channel();
        SocketChannel targetChannel = (sourceChannel == clientChannel) ? remoteChannel : clientChannel;
        ByteBuffer sourceBuffer = (sourceChannel == clientChannel) ? clientBuffer : remoteBuffer;
        
        // 更新活動時間
        lastActivityTime = System.currentTimeMillis();
        
        // 讀取數據
        sourceBuffer.clear();
        int bytesRead = sourceChannel.read(sourceBuffer);
        
        if (bytesRead == -1) {
            // 連接關閉
            logger.debug("連接關閉 [{}]", connectionId);
            close();
            return;
        }
        
        if (bytesRead > 0) {
            // 檢查封包大小
            if (!securityManager.isPacketSizeValid(bytesRead)) {
                logger.warn("封包大小異常 [{}]: {} bytes", connectionId, bytesRead);
                close();
                return;
            }
            
            // 轉發數據
            sourceBuffer.flip();
            while (sourceBuffer.hasRemaining()) {
                int bytesWritten = targetChannel.write(sourceBuffer);
                if (bytesWritten == 0) {
                    // 寫入緩衝區滿，等待下次寫入
                    break;
                }
            }
            
            logger.debug("轉發數據 [{}]: {} bytes", connectionId, bytesRead);
        }
    }
    
    /**
     * 檢查連接超時
     */
    private void checkTimeout() {
        if (securityManager.isTcpPingAttack(connectionTime, lastActivityTime)) {
            logger.warn("檢測到TCP ping攻擊，關閉連接 [{}]", connectionId);
            close();
        }
    }
    
    /**
     * 關閉連接
     */
    public void close() {
        if (closed) {
            return;
        }
        
        closed = true;
        
        try {
            // 釋放安全管理器資源
            securityManager.releaseConnection(clientAddress);
            
            // 關閉通道
            if (clientChannel != null && clientChannel.isOpen()) {
                clientChannel.close();
            }
            if (remoteChannel != null && remoteChannel.isOpen()) {
                remoteChannel.close();
            }
            
            logger.info("連接已關閉 [{}]", connectionId);
            
        } catch (IOException e) {
            logger.error("關閉連接失敗 [{}]", connectionId, e);
        }
    }
    
    /**
     * 獲取客戶端地址（優先使用Proxy Protocol中的地址）
     */
    public InetAddress getClientAddress() {
        return proxyInfo != null ? proxyInfo.getSourceAddress() : clientAddress;
    }
    
    /**
     * 獲取連接ID
     */
    public long getConnectionId() {
        return connectionId;
    }
    
    /**
     * 檢查連接是否已關閉
     */
    public boolean isClosed() {
        return closed;
    }
}