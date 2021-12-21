package com.rtsp.module.netty.module;

import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rtsp.config.ConfigManager;
import com.rtsp.module.netty.handler.RtcpChannelHandler;
import com.rtsp.service.AppInstance;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @class public class NettyChannel
 * @brief NettyChannel class
 */
public class RtcpNettyChannel { // > UDP

    private static final Logger logger = LoggerFactory.getLogger(RtcpNettyChannel.class);

    private final String rtspUnitId;
    private final String listenIp;
    private final int listenPort;

    private Bootstrap b;
    private NioEventLoopGroup group;
    /*메시지 수신용 채널 */
    private Channel serverChannel;

    ////////////////////////////////////////////////////////////////////////////////

    public RtcpNettyChannel(String rtspUnitId, String ip, int port) {
        this.rtspUnitId = rtspUnitId;
        this.listenIp = ip;
        this.listenPort = port;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public void run (String ip, int port) {
        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        int nioThreadCount = configManager.getStreamThreadPoolSize();
        int sendBufSize = configManager.getSendBufSize();
        int recvBufSize = configManager.getRecvBufSize();

        group = new NioEventLoopGroup(nioThreadCount);
        b = new Bootstrap();
        b.group(group).channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, false)
                .option(ChannelOption.SO_SNDBUF, sendBufSize)
                .option(ChannelOption.SO_RCVBUF, recvBufSize)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    protected void initChannel(NioDatagramChannel socketChannel) {
                        final ChannelPipeline pipeline = socketChannel.pipeline();
                        pipeline.addLast(
                                new RtcpChannelHandler(
                                        rtspUnitId,
                                        ip,
                                        port
                                )
                        );
                    }
                });
    }

    /**
     * @fn public void stop()
     * @brief Netty Channel 을 종료하는 함수
     */
    public void stop () {
        if (group != null) {
            group.shutdownGracefully();
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @param ip   바인딩할 ip
     * @param port 바인당할 port
     * @return 성공 시 생성된 Channel, 실패 시 null 반환
     * @fn public Channel openChannel(String ip, int port)
     * @brief Netty Server Channel 을 생성하는 함수
     */
    public Channel openChannel (String ip, int port) {
        if (serverChannel != null) {
            logger.warn("Channel is already opened.");
            return null;
        }

        InetAddress address;
        ChannelFuture channelFuture;

        try {
            address = InetAddress.getByName(ip);
        } catch (UnknownHostException e) {
            logger.warn("UnknownHostException is occurred. (ip={})", ip, e);
            return null;
        }

        try {
            channelFuture = b.bind(address, port).sync();
            serverChannel = channelFuture.channel();
            logger.debug("Channel is opened. (ip={}, port={})", address, port);

            return channelFuture.channel();
        } catch (Exception e) {
            logger.warn("Channel is interrupted. (address={}:{})", ip, port, e);
            return null;
        }
    }

    /**
     * @fn public void closeChannel()
     * @brief Netty Server Channel 을 닫는 함수
     */
    public void closeChannel ( ) {
        if (serverChannel == null) {
            logger.warn("Channel is already closed.");
            return;
        }

        serverChannel.close();
        serverChannel = null;
        logger.debug("Channel is closed.");
    }

    public String getListenIp() {
        return listenIp;
    }

    public int getListenPort() {
        return listenPort;
    }

}
