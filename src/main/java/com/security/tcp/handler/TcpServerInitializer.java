package com.security.tcp.handler;

import com.security.tcp.config.ServerConfig;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.concurrent.TimeUnit;

/**
 * TCP服務器通道初始化器
 */
public class TcpServerInitializer extends ChannelInitializer<SocketChannel> {
    
    private final ServerConfig config = ServerConfig.getInstance();
    
    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        
        // 1. 空閒檢測處理器
        pipeline.addLast("idleStateHandler", new IdleStateHandler(
                config.getReadTimeoutSeconds(), 
                config.getWriteTimeoutSeconds(), 
                0, 
                TimeUnit.SECONDS));
        
        // 2. 連接管理處理器（檢查CC攻擊）
        pipeline.addLast("connectionHandler", new ConnectionHandler());
        
        // 3. 封包驗證處理器
        if (config.isPacketValidationEnabled()) {
            pipeline.addLast("packetValidator", new PacketValidationHandler());
        }
        
        // 4. 速率限制處理器
        if (config.isCcProtectionEnabled()) {
            pipeline.addLast("rateLimitHandler", new RateLimitHandler());
        }
        
        // 5. 業務處理器（這裡是echo服務器示例）
        pipeline.addLast("businessHandler", new BusinessHandler());
        
        // 6. 異常處理器
        pipeline.addLast("exceptionHandler", new ExceptionHandler());
    }
}