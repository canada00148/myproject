package com.security.tcp.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * 全局異常處理器
 */
public class ExceptionHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandler.class);
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientInfo = remoteAddress != null ? remoteAddress.toString() : "unknown";
        
        if (cause instanceof IOException) {
            // IO異常通常是客戶端斷開連接
            logger.debug("IO exception from {}: {}", clientInfo, cause.getMessage());
        } else {
            // 其他異常需要記錄詳細信息
            logger.error("Unexpected exception from {}: ", clientInfo, cause);
        }
        
        // 關閉連接
        ctx.close();
    }
}