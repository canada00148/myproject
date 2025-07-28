package com.portforward.protocol;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

public class ProxyProtocolParser {
    private static final Logger logger = LoggerFactory.getLogger(ProxyProtocolParser.class);
    
    // Proxy Protocol V1 魔術字節
    private static final String PROXY_PROTOCOL_V1_MAGIC = "PROXY ";
    private static final byte[] PROXY_PROTOCOL_V1_MAGIC_BYTES = PROXY_PROTOCOL_V1_MAGIC.getBytes();
    
    public static class ProxyInfo {
        private final String protocol;
        private final InetAddress sourceAddress;
        private final InetAddress destinationAddress;
        private final int sourcePort;
        private final int destinationPort;
        private final int headerLength;
        
        public ProxyInfo(String protocol, InetAddress sourceAddress, InetAddress destinationAddress, 
                        int sourcePort, int destinationPort, int headerLength) {
            this.protocol = protocol;
            this.sourceAddress = sourceAddress;
            this.destinationAddress = destinationAddress;
            this.sourcePort = sourcePort;
            this.destinationPort = destinationPort;
            this.headerLength = headerLength;
        }
        
        public String getProtocol() { return protocol; }
        public InetAddress getSourceAddress() { return sourceAddress; }
        public InetAddress getDestinationAddress() { return destinationAddress; }
        public int getSourcePort() { return sourcePort; }
        public int getDestinationPort() { return destinationPort; }
        public int getHeaderLength() { return headerLength; }
        
        @Override
        public String toString() {
            return String.format("ProxyInfo{protocol='%s', source=%s:%d, destination=%s:%d, headerLength=%d}",
                    protocol, sourceAddress.getHostAddress(), sourcePort, 
                    destinationAddress.getHostAddress(), destinationPort, headerLength);
        }
    }
    
    /**
     * 檢查並解析Proxy Protocol V1頭部
     * @param channel Socket通道
     * @return ProxyInfo對象，如果不是Proxy Protocol則返回null
     */
    public static ProxyInfo parseProxyProtocol(SocketChannel channel) throws IOException {
        // 創建一個小的緩衝區來讀取魔術字節
        ByteBuffer magicBuffer = ByteBuffer.allocate(PROXY_PROTOCOL_V1_MAGIC_BYTES.length);
        
        // 設置非阻塞模式進行試探性讀取
        channel.configureBlocking(false);
        
        try {
            // 讀取魔術字節
            int bytesRead = channel.read(magicBuffer);
            if (bytesRead < PROXY_PROTOCOL_V1_MAGIC_BYTES.length) {
                // 數據不足，不是Proxy Protocol
                return null;
            }
            
            magicBuffer.flip();
            byte[] receivedMagic = new byte[PROXY_PROTOCOL_V1_MAGIC_BYTES.length];
            magicBuffer.get(receivedMagic);
            
            // 檢查魔術字節
            if (!java.util.Arrays.equals(receivedMagic, PROXY_PROTOCOL_V1_MAGIC_BYTES)) {
                // 不是Proxy Protocol，將數據放回緩衝區
                channel.configureBlocking(true);
                return null;
            }
            
            // 是Proxy Protocol，繼續解析
            return parseProxyProtocolHeader(channel);
            
        } finally {
            // 恢復阻塞模式
            channel.configureBlocking(true);
        }
    }
    
    private static ProxyInfo parseProxyProtocolHeader(SocketChannel channel) throws IOException {
        // 讀取完整的Proxy Protocol頭部
        StringBuilder headerBuilder = new StringBuilder();
        ByteBuffer buffer = ByteBuffer.allocate(1);
        
        // 讀取到換行符
        while (true) {
            buffer.clear();
            int bytesRead = channel.read(buffer);
            if (bytesRead <= 0) {
                throw new IOException("無法讀取完整的Proxy Protocol頭部");
            }
            
            buffer.flip();
            char c = (char) buffer.get();
            headerBuilder.append(c);
            
            if (c == '\n') {
                break;
            }
        }
        
        String header = headerBuilder.toString().trim();
        logger.debug("解析Proxy Protocol頭部: {}", header);
        
        // 解析頭部格式: PROXY protocol src_addr dst_addr src_port dst_port
        String[] parts = header.split("\\s+");
        if (parts.length != 6) {
            throw new IOException("無效的Proxy Protocol頭部格式: " + header);
        }
        
        String protocol = parts[1];
        String sourceAddress = parts[2];
        String destinationAddress = parts[3];
        int sourcePort = Integer.parseInt(parts[4]);
        int destinationPort = Integer.parseInt(parts[5]);
        
        // 解析IP地址
        InetAddress srcAddr = InetAddress.getByName(sourceAddress);
        InetAddress dstAddr = InetAddress.getByName(destinationAddress);
        
        ProxyInfo proxyInfo = new ProxyInfo(protocol, srcAddr, dstAddr, sourcePort, destinationPort, header.length());
        logger.info("解析到Proxy Protocol信息: {}", proxyInfo);
        
        return proxyInfo;
    }
    
    /**
     * 從SocketChannel中跳過Proxy Protocol頭部
     * @param channel Socket通道
     * @param proxyInfo Proxy信息
     */
    public static void skipProxyProtocolHeader(SocketChannel channel, ProxyInfo proxyInfo) throws IOException {
        if (proxyInfo == null) {
            return;
        }
        
        // 已經讀取了魔術字節，需要跳過剩餘的頭部
        int remainingHeaderLength = proxyInfo.getHeaderLength() - PROXY_PROTOCOL_V1_MAGIC_BYTES.length;
        
        if (remainingHeaderLength > 0) {
            ByteBuffer skipBuffer = ByteBuffer.allocate(remainingHeaderLength);
            int bytesRead = channel.read(skipBuffer);
            if (bytesRead != remainingHeaderLength) {
                throw new IOException("無法跳過完整的Proxy Protocol頭部");
            }
        }
    }
}