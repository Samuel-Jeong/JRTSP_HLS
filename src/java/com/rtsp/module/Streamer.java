package com.rtsp.module;

import io.lindstrom.m3u8.model.MediaSegment;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rtsp.config.ConfigManager;
import com.rtsp.module.netty.handler.StreamerChannelHandler;
import com.rtsp.service.AppInstance;

import java.io.File;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Random;

/**
 * @class public class Streamer
 * @brief Streamer class
 */
public class Streamer {

    private static final Logger logger = LoggerFactory.getLogger(Streamer.class);

    /* Streamer id */
    private final String sessionId;

    /* 메시지 송신용 채널 */
    private Channel channel;

    private final String listenIp;
    private final int listenPort;
    private String destIp = null;
    private int destPort = 0; // rtp destination port
    private int rtcpDestPort = 0; // rtcp destination port

    private VideoStream video = null;
    private String uri = null;

    private File m3u8File = null;

    private NioEventLoopGroup group = null;
    private final Bootstrap b = new Bootstrap();

    private final Random random = new Random();

    private int ssrc;
    private int curSeqNum;
    private int curTimeStamp;

    private boolean isMediaEnabled = false;
    private List<MediaSegment> mediaSegmentList = null;
    private String m3u8PathOnly = null;
    private int curMediaSegmentCount = 0;

    /////////////////////////////////////////////////////////////////////

    public Streamer(String sessionId, String listenIp, int listenPort) {
        this.sessionId = sessionId;

        this.listenIp = listenIp;
        this.listenPort = listenPort;

        ssrc = random.nextInt(Integer.MAX_VALUE);
        curSeqNum = random.nextInt(5000);
        curTimeStamp = random.nextInt(5000);

        logger.debug("({}) Streamer is created. (listenIp={}, listenPort={}, uri={})", sessionId, listenIp, listenPort, uri);
    }

    /////////////////////////////////////////////////////////////////////

    public Streamer init() {
        if (channel != null) {
            return null;
        }

        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        group = new NioEventLoopGroup(configManager.getStreamThreadPoolSize());
        b.group(group).channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, false)
                .option(ChannelOption.SO_SNDBUF, configManager.getSendBufSize())
                .option(ChannelOption.SO_RCVBUF, configManager.getRecvBufSize())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 1000)
                .handler(new ChannelInitializer<NioDatagramChannel>() {
                    @Override
                    public void initChannel (final NioDatagramChannel ch) {
                        final ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(
                                //new DefaultEventExecutorGroup(1),
                                new StreamerChannelHandler(
                                        sessionId
                                )
                        );
                    }
                });

        return this;
    }

    public void start () {
        try {
            if (m3u8File == null) {
                String destFilePath = video.getResultM3U8FilePath();
                m3u8File = new File(destFilePath);
            }

            InetAddress address = InetAddress.getByName(destIp);
            ChannelFuture channelFuture = b.connect(address, destPort).sync();
            channelFuture.addListener(
                    (ChannelFutureListener) future -> logger.debug("({}) Success to connect with remote peer. (ip={}, port={})", sessionId, destIp, destPort)
            );
            channel = channelFuture.channel();

            logger.debug("({}) Streamer is started. ({})", sessionId, this);
        } catch (Exception e) {
            logger.warn("({}) Streamer.start.Exception", sessionId, e);
        }
    }

    public void stop () {
        if (channel != null) {
            channel.closeFuture();
            channel.close();
            channel = null;
        }

        logger.debug("({}) Streamer is stopped. ({})", sessionId, this);
    }

    private void removeFile(File file) {
        if (file.exists()) {
            if (file.delete()) {
                logger.trace("({}) Success to remove. (file={})", sessionId, file.getAbsolutePath());
            } else {
                logger.warn("({}) Fail to remove files. (file={})", sessionId, file.getAbsolutePath());
            }
        }
    }

    public void finish () {
        stop();

        if (m3u8File != null) {
            removeFile(m3u8File);
            m3u8File = null;
        }

        if (mediaSegmentList != null && !mediaSegmentList.isEmpty()) {
            for (MediaSegment mediaSegment : mediaSegmentList) {
                String tsFileName = mediaSegment.uri();
                tsFileName = m3u8PathOnly + File.separator + tsFileName;
                removeFile(new File(tsFileName.trim()));
            }
        }

        if (group != null) {
            group.shutdownGracefully();
        }

        logger.debug("({}) Streamer is finished. ({})", sessionId, this);
    }

    /////////////////////////////////////////////////////////////////////

    public String getDestIp() {
        return destIp;
    }

    public void setDestIp(String destIp) {
        this.destIp = destIp;
        logger.debug("({}) Streamer destIp is set up. ({})", sessionId, destIp);
    }

    public int getDestPort() {
        return destPort;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
        logger.debug("({}) Streamer destPort is set up. ({})", sessionId, destPort);
    }

    public int getRtcpDestPort() {
        return rtcpDestPort;
    }

    public void setRtcpDestPort(int rtcpDestPort) {
        this.rtcpDestPort = rtcpDestPort;
        logger.debug("({}) Streamer rtcpDestPort is set up. ({})", sessionId, rtcpDestPort);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        if (uri.charAt(uri.length() - 1) == '/') {
            uri = uri.substring(
                    0,
                    uri.length() - 1
            );
        }

        if (this.uri == null) {
            if (!uri.endsWith(".mp4")) {
                logger.warn("({}) Fail to set the first uri. URI format is not mp4. (listenIp={}, listenPort={}, uri={})",
                        sessionId, listenIp, listenPort, uri
                );
                return;
            }

            this.uri = uri;
            this.video = new VideoStream(
                    uri
            );
            logger.debug("({}) Streamer the first uri is set up. ({})", sessionId, uri);
        }

        if (!this.uri.equals(uri)) {
            this.uri = uri;
            logger.debug("({}) Streamer the next uri is set up. ({})", sessionId, uri);
        }
    }

    public List<MediaSegment> getMediaSegmentList() {
        return mediaSegmentList;
    }

    public void setMediaSegmentList(List<MediaSegment> mediaSegmentList) {
        this.mediaSegmentList = mediaSegmentList;
    }

    public String getM3u8PathOnly() {
        return m3u8PathOnly;
    }

    public void setM3u8PathOnly(String m3u8PathOnly) {
        this.m3u8PathOnly = m3u8PathOnly;
        logger.debug("({}) Streamer m3u8PathOnly is set up. ({})", sessionId, m3u8PathOnly);
    }

    public int getCurMediaSegmentCount() {
        return curMediaSegmentCount;
    }

    public void setCurMediaSegmentCount(int curMediaSegmentCount) {
        this.curMediaSegmentCount = curMediaSegmentCount;
    }

    public boolean isMediaEnabled() {
        return isMediaEnabled;
    }

    public void setMediaEnabled(boolean isMediaEnabled) {
        this.isMediaEnabled = isMediaEnabled;
        logger.debug("({}) Streamer mediaEnabled is set up. ({})", sessionId, isMediaEnabled);
    }

    public int getSsrc() {
        return ssrc;
    }

    public void setSsrc() {
        this.ssrc = random.nextInt(Integer.MAX_VALUE);
        logger.debug("({}) Streamer ssrc is set up. ({})", sessionId, ssrc);
    }

    public int getCurSeqNum() {
        return curSeqNum;
    }

    public void setCurSeqNum(int curSeqNum) {
        this.curSeqNum = curSeqNum;
    }

    public int getCurTimeStamp() {
        return curTimeStamp;
    }

    public void setCurTimeStamp(int curTimeStamp) {
        this.curTimeStamp = curTimeStamp;
    }

    public String getSessionId() {
        return sessionId;
    }

    public VideoStream getVideo() {
        return video;
    }

    /**
     * @fn public boolean isActive()
     * @brief Streamer 활성화 여부를 반환하는 함수
     * @return Streamer 활성화 여부를 반환
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
        return "Streamer{" +
                "id='" + sessionId + '\'' +
                ", channel=" + channel +
                ", listenIp='" + listenIp + '\'' +
                ", listenPort=" + listenPort +
                ", destIp='" + destIp + '\'' +
                ", destPort=" + destPort +
                ", rtcpDestPort=" + rtcpDestPort +
                ", video=" + video +
                ", uri='" + uri + '\'' +
                ", m3u8File=" + m3u8File +
                '}';
    }
}
