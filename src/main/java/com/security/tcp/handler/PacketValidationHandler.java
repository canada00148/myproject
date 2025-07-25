package com.security.tcp.handler;

import com.security.tcp.config.ServerConfig;
import com.security.tcp.model.ConnectionInfo;
import com.security.tcp.service.ConnectionManager;
import com.security.tcp.service.PacketValidator;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

/**
 * 封包驗證處理器
 */
public class PacketValidationHandler extends ChannelInboundHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(PacketValidationHandler.class);
    private final PacketValidator validator = PacketValidator.getInstance();
    private final ConnectionManager connectionManager = ConnectionManager.getInstance();
    private final ServerConfig config = ServerConfig.getInstance();
    
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof ByteBuf)) {
            super.channelRead(ctx, msg);
            return;
        }
        
        ByteBuf packet = (ByteBuf) msg;
        InetSocketAddress remoteAddress = (InetSocketAddress) ctx.channel().remoteAddress();
        String clientIp = remoteAddress.getAddress().getHostAddress();
        
        // 獲取連接信息
        ConnectionInfo connInfo = connectionManager.getConnectionInfo(ctx.channel());
        if (connInfo != null) {
            connInfo.addBytesReceived(packet.readableBytes());
        }
        
        // 驗證封包
        PacketValidator.ValidationResult result = validator.validatePacket(packet, clientIp);
        
        if (!result.isValid()) {
            logger.error("Invalid packet from {}: {}", clientIp, result.getErrors());
            
            // 標記連接為可疑
            if (connInfo != null) {
                connInfo.markSuspicious("Invalid packet: " + result.getErrors());
            }
            
            // 根據配置決定是否丟棄封包
            if (config.isDropInvalidPackets()) {
                packet.release();
                
                // 如果檢測到攻擊，可以選擇關閉連接
                if (result.getAttackType() != null) {
                    logger.error("Attack detected from {}: {}", clientIp, result.getAttackType());
                    ctx.close();
                }
                return;
            }
        }
        
        // 記錄警告
        if (!result.getWarnings().isEmpty()) {
            logger.warn("Packet warnings from {}: {}", clientIp, result.getWarnings());
        }
        
        // 傳遞給下一個處理器
        super.channelRead(ctx, msg);
    }
}