package com.rtsp.module;

import io.lindstrom.m3u8.model.MediaSegment;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.DatagramPacket;
import io.netty.channel.socket.nio.NioDatagramChannel;
import org.apache.commons.lang3.time.StopWatch;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @class public class Streamer
 * @brief Streamer class
 */
public class Streamer {

    private static final Logger logger = LoggerFactory.getLogger(Streamer.class);

    private String clientUserAgent = null;

    private NioEventLoopGroup group = null;
    private final Bootstrap b = new Bootstrap();

    private final String sessionId; /* Streamer id */
    private Channel channel; /* 메시지 송신용 채널 */

    private final String listenIp;
    private final int listenPort;
    private String destIp = null;
    private int destPort = 0; // rtp destination port
    private int rtcpDestPort = 0; // rtcp destination port

    private VideoStream video = null;
    private String uri = null;
    private File m3u8File = null;
    private final Random random = new Random();
    private int ssrc;
    private int curSeqNum;
    private long curTimeStamp;

    private List<MediaSegment> mediaSegmentList = null;
    private String m3u8PathOnly = null;

    private final AtomicBoolean isPaused = new AtomicBoolean(false);
    private final AtomicLong pausedTime = new AtomicLong(0);
    private final StopWatch stopWatch = new StopWatch();

    /////////////////////////////////////////////////////////////////////

    public Streamer(String sessionId, String listenIp, int listenPort) {
        this.sessionId = sessionId;

        this.listenIp = listenIp;
        this.listenPort = listenPort;

        ssrc = random.nextInt(Integer.MAX_VALUE);
        curSeqNum = random.nextInt(100);
        curTimeStamp = random.nextInt(100);

        logger.debug("({}) Streamer is created. (listenIp={}, listenPort={}, uri={})", sessionId, listenIp, listenPort, uri);
    }

    /////////////////////////////////////////////////////////////////////

    public Streamer init() {
        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        group = new NioEventLoopGroup(configManager.getStreamThreadPoolSize());
        b.group(group).channel(NioDatagramChannel.class)
                .option(ChannelOption.SO_BROADCAST, false)
                .option(ChannelOption.SO_SNDBUF, configManager.getSendBufSize())
                .option(ChannelOption.SO_RCVBUF, configManager.getRecvBufSize())
                .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                .option(ChannelOption.SO_REUSEADDR, true)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000)
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

    public void open() {
        try {
            if (m3u8File == null) {
                String destFilePath = video.getResultM3U8FilePath();
                m3u8File = new File(destFilePath);
            }

            InetAddress address = InetAddress.getByName(destIp);
            ChannelFuture channelFuture = b.connect(address, destPort).sync();
            channelFuture.addListener(
                    (ChannelFutureListener) future -> logger.trace("({}) Success to connect with remote peer. (ip={}, port={})", sessionId, destIp, destPort)
            );
            channel = channelFuture.channel();

            if (isPaused.get()) {
                isPaused.set(false);
                setPausedTime(0);
            }
            //logger.debug("({}) Streamer is started. ({})", sessionId, this);
        } catch (Exception e) {
            logger.warn("({}) Streamer.start.Exception", sessionId, e);
        }
    }

    public String getClientUserAgent() {
        return clientUserAgent;
    }

    public void setClientUserAgent(String clientUserAgent) {
        this.clientUserAgent = clientUserAgent;
    }

    public boolean isPaused () {
        return isPaused.get();
    }

    public StopWatch getStopWatch() {
        return stopWatch;
    }

    public void setPausedTime (long pausedTime) {
        this.pausedTime.set(pausedTime);
        //logger.debug("({}) Set paused time. ({})", sessionId, pausedTime);
    }

    public long getPausedTime () {
        return pausedTime.get();
    }

    public void pause () {
        if (channel == null) {
            return;
        }

        isPaused.set(true);
        //logger.debug("({}) Streamer is paused. ({})", sessionId, this);
    }

    public void close () {
        if (channel != null) {
            channel.closeFuture();
            channel.close();
            channel = null;
        }
    }

    public void stop () {
        close();
        isPaused.set(true);
        setPausedTime(0);

        if (AppInstance.getInstance().getConfigManager().isDeleteM3u8()) {
            if (m3u8File != null) {
                removeFile(m3u8File);
                m3u8File = null;
            }
        }

        if (AppInstance.getInstance().getConfigManager().isDeleteTs()) {
            if (mediaSegmentList != null && !mediaSegmentList.isEmpty()) {
                for (MediaSegment mediaSegment : mediaSegmentList) {
                    String tsFileName = mediaSegment.uri();
                    tsFileName = m3u8PathOnly + File.separator + tsFileName;
                    removeFile(new File(tsFileName.trim()));
                }
            }
        }

        //logger.debug("({}) Streamer is stopped. ({})", sessionId, this);
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

    public long getCurTimeStamp() {
        return curTimeStamp;
    }

    public void setCurTimeStamp(long curTimeStamp) {
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
            /*if (channel.isActive()) {
                logger.debug("({}) channel active", sessionId);
            } else {
                logger.warn("({}) channel inactive", sessionId);
            }

            if (channel.isOpen()) {
                logger.debug("({}) channel opened", sessionId);
            } else {
                logger.warn("({}) channel closed", sessionId);
            }*/

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
        close();
        open();

        if (!isActive()) {
            logger.warn("({}) Fail to send the message. Channel is inactive. (ip={}, port={})", sessionId, ip, port);
            return;
        }

        try {
            if (buf == null || ip == null || port <= 0) {
                logger.warn("({}) Fail to send the message. (ip={}, port={})", sessionId, ip, port);
                return;
            }

            if (channel != null) {
                InetSocketAddress addr = new InetSocketAddress(ip, port);
                ChannelFuture channelFuture = channel.writeAndFlush(new DatagramPacket(buf, addr));
                if (channelFuture == null) {
                    logger.warn("({}) Fail to send the message. (ip={}, port={})", sessionId, ip, port);
                }
            }
        } catch (Exception e) {
            logger.warn("({}) Streamer.send.Exception", sessionId, e);
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
