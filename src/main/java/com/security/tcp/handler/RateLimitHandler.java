package com.security.tcp.handler;

import com.security.tcp.service.RateLimitService;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * 速率限制處理器
 */
public class RateLimitHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimitHandler.class);
    private final RateLimitService rateLimitService = RateLimitService.getInstance();
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientIp = remoteAddress.getAddress().getHostAddress();
        
        // 檢查全局速率限制
        if (!rateLimitService.allowGlobalRequest()) {
            logger.warn("Global rate limit exceeded, dropping packet from: {}", clientIp);
            return;
        }
        
        // 檢查IP速率限制
        if (!rateLimitService.allowRequestFromIp(clientIp)) {
            logger.warn("IP rate limit exceeded for: {}", clientIp);
            // 可以選擇關閉連接或只是丟棄當前封包
            ctx.close();
            return;
        }
        
        // 檢查時間窗口限制
        String windowKey = "window:" + clientIp;
        if (!rateLimitService.checkWindowLimit(windowKey, 1000, 60)) {
            logger.warn("Window rate limit exceeded for: {}", clientIp);
            return;
        }
        
        // 傳遞給下一個處理器
        super.channelRead(ctx, msg);
    }
}