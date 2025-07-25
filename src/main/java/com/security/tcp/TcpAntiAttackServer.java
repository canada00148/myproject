package com.security.tcp;

import com.security.tcp.config.ServerConfig;
import com.security.tcp.handler.TcpServerInitializer;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TCP 抗攻擊伺服器主類
 * 使用Netty實現高性能的TCP伺服器，支持CC防護和封包檢測
 */
public class TcpAntiAttackServer {
    private static final Logger logger = LoggerFactory.getLogger(TcpAntiAttackServer.class);
    
    private final int port;
    private EventLoopGroup bossGroup;
    private EventLoopGroup workerGroup;
    
    public TcpAntiAttackServer(int port) {
        this.port = port;
    }
    
    public void start() throws InterruptedException {
        // 配置伺服器
        ServerConfig config = ServerConfig.getInstance();
        
        // Boss組處理連接請求，Worker組處理I/O操作
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup(config.getWorkerThreads());
        
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childHandler(new TcpServerInitializer());
            
            // 綁定端口並啟動伺服器
            ChannelFuture future = bootstrap.bind(port).sync();
            logger.info("TCP Anti-Attack Server started on port {}", port);
            logger.info("Protection enabled - CC Defense: {}, Packet Validation: {}", 
                    config.isCcProtectionEnabled(), config.isPacketValidationEnabled());
            
            // 等待伺服器關閉
            future.channel().closeFuture().sync();
        } finally {
            shutdown();
        }
    }
    
    public void shutdown() {
        logger.info("Shutting down TCP Anti-Attack Server...");
        if (bossGroup != null) {
            bossGroup.shutdownGracefully();
        }
        if (workerGroup != null) {
            workerGroup.shutdownGracefully();
        }
    }
    
    public static void main(String[] args) {
        int port = 8888; // 默認端口
        
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                logger.error("Invalid port number: {}", args[0]);
                System.exit(1);
            }
        }
        
        try {
            new TcpAntiAttackServer(port).start();
        } catch (InterruptedException e) {
            logger.error("Server interrupted", e);
        }
    }
}