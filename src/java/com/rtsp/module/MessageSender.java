package com.rtsp.module;

import com.rtsp.module.base.RtspUnit;
import com.rtsp.module.netty.handler.MessageSenderChannelHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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

    private String destIp = null;
    private int destPort = 0;
    private int rtcpDestPort = 0;

    private final VideoStream video;
    private final String fileName;

    private File m3u8File;

    private NioEventLoopGroup group;
    private final Bootstrap b = new Bootstrap();

    /////////////////////////////////////////////////////////////////////

    public MessageSender(String id, String listenIp, int listenPort, String fileName) {
        this.id = id;

        this.listenIp = listenIp;
        this.listenPort = listenPort;

        this.video = new VideoStream(
                fileName
        );

        this.fileName = fileName;
    }

    /////////////////////////////////////////////////////////////////////

    public MessageSender init() {
        if (channel != null) {
            return null;
        }

        group = new NioEventLoopGroup(100);
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

        RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
        if (rtspUnit != null) {
            rtspUnit.setSsrc();
        }

        return this;
    }

    public void start (String destIp, int destPort, int rtcpDestPort) {
        try {
            if (!destIp.equals(this.destIp)) {
                logger.debug("Dest IP is changed. ({} > {})", this.destIp, destIp);
                this.destIp = destIp;
            }

            if (destPort != this.destPort) {
                logger.debug("Dest Port is changed. ({} > {})", this.destPort, destPort);
                this.destPort = destPort;
            }

            if (rtcpDestPort != this.rtcpDestPort) {
                logger.debug("Dest Rtcp Port is changed. ({} > {})", this.rtcpDestPort, rtcpDestPort);
                this.rtcpDestPort = rtcpDestPort;
            }

            if (m3u8File == null) {
                String destFilePath = video.getResultM3U8FilePath();
                m3u8File = new File(destFilePath);
            }

            InetAddress address = InetAddress.getByName(destIp);
            ChannelFuture channelFuture = b.connect(address, destPort).sync();
            channelFuture.addListener(
                    (ChannelFutureListener) future -> logger.debug("Success to connect with remote peer. (ip={}, port={})", destIp, destPort)
            );
            channel = channelFuture.channel();
        } catch (Exception e) {
            logger.warn("MessageSender.start.Exception", e);
        }
    }

    public void stop () {
        if (m3u8File != null) {
            removeFile(m3u8File);
            m3u8File = null;
        }

        if (channel != null) {
            channel.closeFuture();
            channel.close();
            channel = null;

            logger.debug("MessageSender is finished.");
        }
    }

    private void removeFile(File file) {
        if (file.exists()) {
            if (file.delete()) {
                logger.trace("Success to remove. (file={})", file.getAbsolutePath());
            } else {
                logger.warn("Fail to remove files. (file={})", file.getAbsolutePath());
            }
        }
    }

    public void finish () {
        stop();

        if (group != null) {
            group.shutdownGracefully();
        }
    }

    /////////////////////////////////////////////////////////////////////

    public VideoStream getVideo() {
        return video;
    }

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
