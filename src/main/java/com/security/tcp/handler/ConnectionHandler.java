package com.security.tcp.handler;

import com.security.tcp.service.ConnectionManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * 連接處理器，處理連接的建立、關閉和空閒
 */
public class ConnectionHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(ConnectionHandler.class);
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientIp = remoteAddress.getAddress().getHostAddress();
        
        // 檢查是否允許新連接
        if (!connectionManager.registerConnection(ctx.channel())) {
            logger.warn("Connection rejected from: {}", clientIp);
            ctx.close();
            return;
        }
        
        logger.info("Connection established from: {}", clientIp);
        super.channelActive(ctx);
    }
    
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        connectionManager.unregisterConnection(ctx.channel());
        super.channelInactive(ctx);
    }
    
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            if (event.state() == IdleState.READER_IDLE) {
                logger.warn("Read idle timeout, closing connection: {}", 
                    ctx.channel().remoteAddress());
                ctx.close();
            } else if (event.state() == IdleState.WRITER_IDLE) {
                logger.warn("Write idle timeout for connection: {}", 
                    ctx.channel().remoteAddress());
            }
        }
        super.userEventTriggered(ctx, evt);
    }
}