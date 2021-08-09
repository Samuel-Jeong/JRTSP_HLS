package com.rtsp.module;

import com.rtsp.module.base.MessageHandler;
import com.rtsp.module.netty.handler.MessageSenderChannelHandler;
import com.rtsp.service.TaskManager;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;

/**
 * @class public class MessageSender
 * @brief MessageSender class
 */
public class MessageSender {

    private static final Logger logger = LoggerFactory.getLogger(MessageSender.class);

    /* MessageSender id */
    private final String id;

    /* 메시지 송신용 채널 */
    private Channel channel;

    private final String listenIp;
    private final int listenPort;

    private final String destIp;
    private final int destPort;
    private final int rtcpDestPort;

    private final String fileName;

    /////////////////////////////////////////////////////////////////////

    public MessageSender(String id, String listenIp, int listenPort, String destIp, int destPort, int rtcpDestPort, String fileName) {
        this.id = id;

        this.listenIp = listenIp;
        this.listenPort = listenPort;

        this.destIp = destIp;
        this.destPort = destPort;
        this.rtcpDestPort = rtcpDestPort;

        this.fileName = fileName;
    }

    /////////////////////////////////////////////////////////////////////

    public MessageSender init() {
        if (channel != null) {
            return null;
        }

        InetAddress address;
        ChannelFuture channelFuture;

        NioEventLoopGroup group = new NioEventLoopGroup(100);
        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, false)
                .option(ChannelOption.SO_SNDBUF, 33554432)
                .option(ChannelOption.SO_RCVBUF, 16777216)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    public void initChannel (final NioDatagramChannel ch) {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(
                                //new DefaultEventExecutorGroup(1),
                                new MessageSenderChannelHandler(
                                        id,
                                        listenIp,
                                        listenPort
                                )
                        );
                    }
                });

        try {
            address = InetAddress.getByName(destIp);
            channelFuture = b.connect(address, destPort).sync();
            channelFuture.addListener(
                    (ChannelFutureListener) future -> logger.debug("Success to connect with remote peer. (ip={}, port={})", destIp, destPort)
            );
            channel = channelFuture.channel();
        } catch (Exception e) {
            logger.warn("MessageSender.start.Exception. (ip={}, port={})", destIp, destPort, e);
            return null;
        }

        return this;
    }

    public void start () {
        TaskManager.getInstance().addTask(
                MessageSender.class.getSimpleName() + "_" + id,
                new MessageHandler(
                        1,
                        id,
                        listenIp,
                        listenPort,
                        destIp,
                        destPort,
                        fileName
                )
        );
    }

    public void stop () {
        if (channel != null) {
             TaskManager.getInstance().removeTask(
                    MessageSender.class.getSimpleName() + "_" + id
            );
        }
    }

    public void finish () {
        if (channel != null) {
            stop();

            channel.closeFuture();
            channel.close();
            channel = null;

            logger.debug("MessageSender is finished.");
        }
    }

    /////////////////////////////////////////////////////////////////////

    /**
     * @fn public boolean isActive()
     * @brief MessageSender 활성화 여부를 반환하는 함수
     * @return MessageSender 활성화 여부를 반환
     */
    public boolean isActive() {
        if (channel != null) {
            return channel.isActive() && channel.isOpen();
        } else {
            return false;
        }
    }

    /**
     * @fn public void send(ByteBuf buf, String ip, int port)
     * @brief 연결된 채널로 지정한 데이터를 송신하는 함수
     * @param buf ByteBuf
     * @param ip Destination IP
     * @param port Destination Port
     */
    public void send(ByteBuf buf, String ip, int port) {
        if (buf == null || ip == null || port <= 0) {
            return;
        }

        if (channel != null) {
            InetSocketAddress addr = new InetSocketAddress(ip, port);
            channel.writeAndFlush(new DatagramPacket(buf, addr));
        }
    }

    /////////////////////////////////////////////////////////////////////


    @Override
    public String toString() {
        return "MessageSender{" +
                "id='" + id + '\'' +
                ", channel=" + channel +
                ", ip='" + destIp + '\'' +
                ", port=" + destPort +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}
