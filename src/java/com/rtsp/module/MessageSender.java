package com.rtsp.module;

import com.rtsp.config.ConfigManager;
import com.rtsp.module.netty.NettyChannelManager;
import com.rtsp.service.AppInstance;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
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
    /* Destination IP */
    private String ip;
    /* Destination Port */
    private int port;

    /////////////////////////////////////////////////////////////////////

    /**
     * @fn public MessageSender(String id, String ip, int port)
     * @brief MessageSender 생성자 함수
     * @param id MessageSender id
     * @param ip Destination IP
     * @param port Destination Port
     */
    public MessageSender(String id, String ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    /////////////////////////////////////////////////////////////////////

    /**
     * @fn public MessageSender start (Bootstrap b)
     * @brief MessageSender 시작 함수
     * MessageSender 호출한 Context 에서 채널 연결 여부를 판단할 수 있도록 MessageSender 객체를 반환
     * @return 성공 시 MessageSender 객체 반환, 실패 시 null 반환
     */
    public MessageSender start () {
        if (channel != null) {
            return null;
        }

        InetAddress address;
        ChannelFuture channelFuture;

        NioEventLoopGroup group = new NioEventLoopGroup(10);
        Bootstrap b = new Bootstrap();
        b.group(group).channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, false)
                .option(ChannelOption.SO_SNDBUF, 33554432)
                .option(ChannelOption.SO_RCVBUF, 16777216)
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000);

        try {
            address = InetAddress.getByName(ip);
            channelFuture = b.connect(address, port).sync();
            channelFuture.addListener(
                    (ChannelFutureListener) future -> logger.trace("| Success to connect with remote peer. (ip={}, port={})", ip, port)
            );
            channel = channelFuture.channel();
        } catch (Exception e) {
            logger.warn("| MessageSender.start.Exception. (ip={}, port={})", ip, port, e);
            return null;
        }

        return this;
    }

    /**
     * @fn public void stop ()
     * @brief MessageSender 종료 함수
     */
    public void stop () {
        if (channel != null) {
            channel.closeFuture();
            channel.close();
            channel = null;
        }
    }

    /////////////////////////////////////////////////////////////////////

    public String getId() {
        return id;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
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
                "Channel=" + channel +
                ", ip='" + ip + '\'' +
                ", port=" + port +
                '}';
    }
}
