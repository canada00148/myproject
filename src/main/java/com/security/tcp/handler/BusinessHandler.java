package com.security.tcp.handler;

import com.security.tcp.model.ConnectionInfo;
import com.security.tcp.service.ConnectionManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * 業務處理器 - 這裡實現一個簡單的Echo服務器作為示例
 * 實際使用時，可以替換為您的業務邏輯
 */
public class BusinessHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(BusinessHandler.class);
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf)) {
            super.channelRead(ctx, msg);
            return;
        }
        
        ByteBuf in = (ByteBuf) msg;
        
        try {
            // 記錄接收的數據
            ConnectionInfo connInfo = connectionManager.getConnectionInfo(ctx.channel());
            
            // Echo服務：將接收到的數據原樣返回
            // 實際應用中，這裡應該處理您的業務邏輯
            ByteBuf response = processBusinessLogic(in);
            
            if (response != null && response.readableBytes() > 0) {
                ctx.writeAndFlush(response);
                
                // 更新發送字節數
                if (connInfo != null) {
                    connInfo.addBytesSent(response.readableBytes());
                }
            }
            
        } finally {
            // 釋放ByteBuf
            in.release();
        }
    }
    
    /**
     * 處理業務邏輯
     * 這裡是Echo服務器的示例實現
     */
    private ByteBuf processBusinessLogic(ByteBuf input) {
        // 讀取數據
        byte[] data = new byte[input.readableBytes()];
        input.readBytes(data);
        
        // 簡單的Echo：返回相同的數據
        String received = new String(data, StandardCharsets.UTF_8);
        logger.debug("Received: {}", received);
        
        // 可以在這裡添加您的業務處理邏輯
        // 例如：解析協議、處理命令、查詢數據庫等
        
        // 返回響應
        String response = "Echo: " + received;
        return Unpooled.copiedBuffer(response.getBytes(StandardCharsets.UTF_8));
    }
    
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.error("Business handler exception: ", cause);
        ctx.close();
    }
}