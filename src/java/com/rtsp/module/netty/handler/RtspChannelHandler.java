package com.rtsp.module.netty.handler;

import com.fsm.StateManager;
import com.fsm.module.StateHandler;
import com.rtsp.ffmpeg.FfmpegManager;
import com.rtsp.fsm.RtspEvent;
import com.rtsp.fsm.RtspState;
import com.rtsp.module.MessageSender;
import com.rtsp.module.RtspManager;
import com.rtsp.module.VideoStream;
import com.rtsp.module.base.RtspUnit;
import com.rtsp.module.netty.NettyChannelManager;
import com.rtsp.protocol.RtpPacket;
import com.rtsp.service.TaskManager;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.rtsp.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Random;

/**
 * @class public class RtspChannelHandler extends ChannelInboundHandlerAdapter
 * @brief RtspChannelHandler class
 */
public class RtspChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RtspChannelHandler.class);

    private static final int MP2T_TYPE = 33; //RTP payload type for MJPEG video
    private static final String MP2T_TAG = "MP2T"; //RTP payload type for MJPEG video

    private final String name;

    private final String listenIp; // local ip
    private final int listenPort; // local(listen) port

    private final Random random = new Random();

    ////////////////////////////////////////////////////////////////////////////////

    public RtspChannelHandler(String listenIp, int listenPort) {
        this.name = listenIp + ":" + listenPort;

        this.listenIp = listenIp;
        this.listenPort = listenPort;

        logger.debug("RtspChannelHandler is created. (listenIp={}, listenPort={})", listenIp, listenPort);
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) {
        try {
            RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
            if (rtspUnit == null) {
                logger.warn("Fail to get the rtsp unit.");
                return;
            }

            if (msg instanceof DefaultHttpRequest) {
                DefaultHttpRequest req = (DefaultHttpRequest) msg;
                FullHttpResponse res = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0,  RtspResponseStatuses.NOT_FOUND);

                logger.debug("Request: {}", req);

                // 1) OPTIONS
                if (req.method() == RtspMethods.OPTIONS) {
                    logger.debug("< OPTIONS");

                    res.setStatus(RtspResponseStatuses.OK);
                    res.headers().add(
                            RtspHeaderValues.PUBLIC,
                            "DESCRIBE, PAUSE, SETUP, PLAY, TEARDOWN, PAUSE"
                    );
                    sendResponse(ctx, req, res);
                }
                // 2) DESCRIBE
                else if (req.method() == RtspMethods.DESCRIBE) {
                    logger.debug("< DESCRIBE");

                    // Set port to client
                    ByteBuf buf = Unpooled.copiedBuffer(
                            "v=0\r\n" +
                                    "c=IN IP4 " + listenIp + "\r\n" +
                                    "t=0\r\n" +
                                    "m=video 5004 RTP/AVP " + MP2T_TYPE + "\r\n" +
                                    "a=rtpmap:" + MP2T_TYPE + " " + MP2T_TAG + "/90000",
                            CharsetUtil.UTF_8

                    );

                    logger.debug("sdp:\n{}", buf.toString(CharsetUtil.UTF_8));

                    res.setStatus(RtspResponseStatuses.OK);
                    res.headers().add(
                            RtspHeaderNames.CONTENT_TYPE,
                            "application/sdp"
                    );
                    res.headers().add(
                            RtspHeaderNames.CONTENT_LENGTH,
                            buf.writerIndex()
                    );

                    res.content().writeBytes(buf);
                    sendResponse(ctx, req, res);
                }
                // 3) SETUP
                else if (req.method() == RtspMethods.SETUP) {
                    logger.debug("< SETUP");

                    StateHandler rtspStateHandler = StateManager.getInstance().getStateHandler(RtspState.NAME);
                    rtspStateHandler.fire(
                            RtspEvent.SETUP,
                            StateManager.getInstance().getStateUnit(rtspUnit.getRtspStateUnitId())
                    );

                    String transportHeaderContent = req.headers().get(RtspHeaderNames.TRANSPORT);
                    String clientPortString = transportHeaderContent.substring(
                            transportHeaderContent.lastIndexOf(";") + 1
                    );

                    if (clientPortString.startsWith(String.valueOf(RtspHeaderValues.INTERLEAVED))) {
                        // Must send the packet by TCP, not UDP
                        logger.warn("< Unknown method: {}", req.method());
                        ctx.write(res).addListener(ChannelFutureListener.CLOSE);

                        rtspUnit.setInterleaved(true);
                        logger.debug("({}) Interleaved streaming is enabled. (destPort={})", rtspUnit.getSessionId(), rtspUnit.getDestPort());
                        return;
                    } else if (clientPortString.startsWith(String.valueOf(RtspHeaderValues.CLIENT_PORT))) {
                        String rtpDesPortString = clientPortString.substring(
                                clientPortString.lastIndexOf("=") + 1
                        );
                        if (rtpDesPortString.contains("-")) {
                            String rtcpDesPortString = rtpDesPortString.substring(
                                    rtpDesPortString.lastIndexOf("-") + 1
                            );
                            int rtcpDestPort = Integer.parseInt(rtcpDesPortString);
                            if (rtcpDestPort <= 0) {
                                logger.warn("({}) Fail to parse rtcp destination port. (transportHeaderContent={})", rtspUnit.getSessionId(), transportHeaderContent);
                                return;
                            }
                            rtspUnit.setRtcpDestPort(rtcpDestPort);
                            logger.debug("({}) Setup the rtcp destination port. (rtcpDestPort={})", rtspUnit.getSessionId(), rtcpDestPort);

                            rtpDesPortString = rtpDesPortString.substring(
                                    0,
                                    rtpDesPortString.lastIndexOf("-")
                            );
                        }

                        int rtpDestPort = Integer.parseInt(rtpDesPortString);
                        if (rtpDestPort <= 0) {
                            logger.warn("Fail to parse rtp destination port. (transportHeaderContent={})", transportHeaderContent);
                            return;
                        }

                        // TODO: Destination IP
                        rtspUnit.setDestIp(listenIp);
                        rtspUnit.setDestPort(rtpDestPort);
                        rtspUnit.setUri(req.uri());

                        if (!rtspUnit.isMediaEnabled()) {
                            rtspUnit.setSessionId(String.valueOf(random.nextInt(Integer.MAX_VALUE)));
                        }
                    } else {
                        logger.warn("Unknown transport header content. ({})", clientPortString);
                        return;
                    }

                    String sessionId = rtspUnit.getSessionId();
                    int destPort = rtspUnit.getDestPort();
                    if (destPort > 0 && sessionId != null) {
                        //
                        res.headers().add(
                                RtspHeaderNames.SESSION,
                                sessionId
                        );

                        res.headers().add(
                                RtspHeaderNames.TRANSPORT,
                                "RTP/AVP;unicast;client_port=" + destPort
                        );

                        res.setStatus(RtspResponseStatuses.OK);
                        sendResponse(ctx, req, res);
                        //

                        logger.debug("({}) Setup to stream the media. (rtpDestPort={})", rtspUnit.getSessionId(),  rtspUnit.getDestPort());
                    } else {
                        logger.warn("({}) Fail to send the response for SETUP. (rtpDestPort={})", rtspUnit.getSessionId(),  rtspUnit.getDestPort());
                    }
                }
                // 4) PLAY
                else if (req.method() == RtspMethods.PLAY) {
                    logger.debug("< PLAY");

                    StateHandler rtspStateHandler = StateManager.getInstance().getStateHandler(RtspState.NAME);
                    rtspStateHandler.fire(
                            RtspEvent.PLAY,
                            StateManager.getInstance().getStateUnit(rtspUnit.getRtspStateUnitId())
                    );

                    res.setStatus(RtspResponseStatuses.OK);
                    sendResponse(ctx, req, res);

                    //
                    String sessionId = rtspUnit.getSessionId();
                    if (sessionId == null) {
                        logger.warn("Fail to process the PLAY request. SessionId is null. (rtspUnit={})", rtspUnit);
                        return;
                    }

                    if (!rtspUnit.isMediaEnabled()) {
                        NettyChannelManager.getInstance().addMessageSender(
                                sessionId,
                                listenIp,
                                listenPort,
                                rtspUnit.getUri()
                        );

                        // Send the packet
                        int destPort = rtspUnit.getDestPort();
                        if (destPort > 0) {
                            logger.debug("({}) Start to stream the media. (rtpDestPort={})", rtspUnit.getSessionId(), destPort);
                            NettyChannelManager.getInstance().startStreaming(
                                    rtspUnit.getSessionId(),
                                    listenIp,
                                    listenPort,
                                    rtspUnit.getDestIp(),
                                    rtspUnit.getDestPort(),
                                    rtspUnit.getRtcpDestPort()
                            );
                        }

                        MessageSender messageSender = NettyChannelManager.getInstance().getMessageSender(sessionId, listenIp, listenPort);
                        if (messageSender == null) {
                            logger.warn("| MessageSender is null. (key={}, listenIp={}, listenPort={})", sessionId, listenIp, listenPort);
                            return;
                        }

                        if (!messageSender.isActive()) {
                            logger.warn("| MessageSender is not active or deleted. (key={}, listenIp={}, listenPort={})", sessionId, listenIp, listenPort);
                            TaskManager.getInstance().removeTask(
                                    MessageSender.class.getSimpleName() + "_" + sessionId
                            );
                            return;
                        }

                        VideoStream video = messageSender.getVideo();
                        logger.debug("resultM3U8FilePath: {}", video.getResultM3U8FilePath());

                        FfmpegManager ffmpegManager = new FfmpegManager();
                        ffmpegManager.convertMp4ToM3u8(
                                video.getFileName(),
                                video.getResultM3U8FilePath()
                        );

                        byte[] m3u8ByteData = Files.readAllBytes(
                                Paths.get(
                                        video.getResultM3U8FilePath()
                                )
                        );

                        int curSeqNum = rtspUnit.getCurSeqNum();
                        int curTimeStamp = rtspUnit.getCurTimeStamp();
                        RtpPacket rtpPacket = new RtpPacket();
                        rtpPacket.setValue(
                                2, 0, 0, 0, 0, MP2T_TYPE, curSeqNum,
                                curTimeStamp,
                                rtspUnit.getSsrc(),
                                m3u8ByteData,
                                m3u8ByteData.length
                        );
                        rtspUnit.setCurSeqNum(curSeqNum + 1);
                        rtspUnit.setCurTimeStamp(curTimeStamp + 100);

                        // Send the packet
                        byte[] totalRtpData = rtpPacket.getData();
                        ByteBuf buf = Unpooled.copiedBuffer(totalRtpData);
                        messageSender.send(
                                buf,
                                rtspUnit.getDestIp(),
                                rtspUnit.getDestPort()
                        );
                        logger.debug(">> Send M3U8 (destIp={}, destPort={})\n{}(size={})", rtspUnit.getDestIp(), rtspUnit.getDestPort(), new String(m3u8ByteData, StandardCharsets.UTF_8), m3u8ByteData.length);

                        MediaPlaylistParser parser = new MediaPlaylistParser();
                        MediaPlaylist playlist = parser.readPlaylist(Paths.get(video.getResultM3U8FilePath()));
                        if (playlist != null) {
                            String m3u8PathOnly = video.getResultM3U8FilePath();
                            m3u8PathOnly = m3u8PathOnly.substring(
                                    0,
                                    m3u8PathOnly.lastIndexOf("/")
                            );
                            rtspUnit.setM3u8PathOnly(m3u8PathOnly);

                            List<MediaSegment> mediaSegmentList = playlist.mediaSegments();
                            logger.debug("mediaSegmentList: {}", mediaSegmentList);
                            rtspUnit.setMediaSegmentList(mediaSegmentList);
                        }

                        rtspUnit.setMediaEnabled(true);

                        logger.debug("({}) Stop to stream the media. (rtpDestPort={})", rtspUnit.getSessionId(), destPort);
                    } else {
                        int destPort = rtspUnit.getDestPort();
                        if (destPort > 0) {
                            logger.debug("({}) Start to stream the media. (rtpDestPort={})", rtspUnit.getSessionId(), destPort);
                            NettyChannelManager.getInstance().startStreaming(
                                    rtspUnit.getSessionId(),
                                    listenIp,
                                    listenPort,
                                    rtspUnit.getDestIp(),
                                    rtspUnit.getDestPort(),
                                    rtspUnit.getRtcpDestPort()
                            );
                        }

                        MessageSender messageSender = NettyChannelManager.getInstance().getMessageSender(sessionId, listenIp, listenPort);
                        if (messageSender == null) {
                            logger.warn("| MessageSender is null. (key={}, listenIp={}, listenPort={})", sessionId, listenIp, listenPort);
                            return;
                        }

                        if (!messageSender.isActive()) {
                            logger.warn("| MessageSender is not active or deleted. (key={}, listenIp={}, listenPort={})", sessionId, listenIp, listenPort);
                            TaskManager.getInstance().removeTask(
                                    MessageSender.class.getSimpleName() + "_" + sessionId
                            );
                            return;
                        }

                        List<MediaSegment> mediaSegmentList = rtspUnit.getMediaSegmentList();
                        String m3u8PathOnly = rtspUnit.getM3u8PathOnly();

                        for (MediaSegment mediaSegment : mediaSegmentList) {
                            String tsFileName = mediaSegment.uri();
                            tsFileName = m3u8PathOnly + File.separator + tsFileName;
                            String curTsFileName = RtspUnit.getPureFileName(rtspUnit.getUri());
                            if (tsFileName.trim().equals(curTsFileName.trim())) {
                                logger.debug("Selected tsFileName: {}", tsFileName);

                                byte[] tsByteData = Files.readAllBytes(
                                        Paths.get(
                                                tsFileName
                                        )
                                );

                                int partitionValue = 1400;
                                int marker = 0;
                                int totalLength = tsByteData.length;

                                if (partitionValue > totalLength) {
                                    partitionValue = totalLength;
                                }

                                // RTP Packet 제한 바이트 : 1500 > payload Maximum size 는 1,460 (1500 - 20(IP) - 8(UDP) - 12(RTP)) > 1400 으로 제한
                                for (int i = 0; i < totalLength; i += partitionValue) {
                                    int curDataLength;
                                    if ((i + partitionValue) >= totalLength) {
                                        curDataLength = totalLength - i;
                                    } else {
                                        curDataLength = partitionValue;
                                    }

                                    byte[] curData = new byte[curDataLength];
                                    System.arraycopy(tsByteData, i, curData, 0, curDataLength);

                                    int curSeqNum = rtspUnit.getCurSeqNum();
                                    int curTimeStamp = rtspUnit.getCurTimeStamp();
                                    RtpPacket rtpPacket = new RtpPacket();
                                    rtpPacket.setValue(
                                            2, 0, 0, 0, marker, MP2T_TYPE, curSeqNum,
                                            curTimeStamp,
                                            rtspUnit.getSsrc(),
                                            curData,
                                            curData.length
                                    );
                                    rtspUnit.setCurSeqNum(curSeqNum + 1);
                                    rtspUnit.setCurTimeStamp(curTimeStamp + 100);

                                    byte[] totalRtpData = rtpPacket.getData();
                                    ByteBuf buf = Unpooled.copiedBuffer(totalRtpData);
                                    messageSender.send(
                                            buf,
                                            rtspUnit.getDestIp(),
                                            rtspUnit.getDestPort()
                                    );
                                    //logger.debug(">> Send TS (destIp={}, destPort={}, size={})", rtspUnit.getDestIp(), rtspUnit.getDestPort(), totalRtpData.length);

                                    if ((i + partitionValue) >= totalLength) {
                                        marker = 1;
                                    }
                                }

                                break;
                            }
                        }

                        NettyChannelManager.getInstance().stopStreaming(
                                rtspUnit.getSessionId(),
                                listenIp,
                                listenPort
                        );
                        logger.debug("({}) Stop to stream the media. (rtpDestPort={})", rtspUnit.getSessionId(), destPort);
                    }
                    //
                }
                // 5) TEARDOWN
                else if (req.method() == RtspMethods.TEARDOWN) {
                    logger.debug("< TEARDOWN");

                    StateHandler rtspStateHandler = StateManager.getInstance().getStateHandler(RtspState.NAME);
                    rtspStateHandler.fire(
                            RtspEvent.TEARDOWN,
                            StateManager.getInstance().getStateUnit(rtspUnit.getRtspStateUnitId())
                    );

                    res.setStatus(RtspResponseStatuses.OK);
                    sendResponse(ctx, req, res);

                    int destPort = rtspUnit.getDestPort();
                    if (destPort > 0) {
                        logger.debug("({}) Stop the streaming. (rtpDestPort={})", rtspUnit.getSessionId(), destPort);
                        NettyChannelManager.getInstance().deleteMessageSender(
                                rtspUnit.getSessionId(),
                                listenIp,
                                listenPort
                        );
                    }
                }
                // 6) PAUSE
                else if (req.method() == RtspMethods.PAUSE) {
                    logger.debug("< PAUSE");

                    res.setStatus(RtspResponseStatuses.OK);
                    sendResponse(ctx, req, res);

                    int destPort = rtspUnit.getDestPort();
                    if (destPort > 0) {
                        logger.debug("({}) Pause the streaming. (rtpDestPort={})", rtspUnit.getSessionId(), destPort);
                        NettyChannelManager.getInstance().stopStreaming(
                                rtspUnit.getSessionId(),
                                listenIp,
                                listenPort
                        );
                    }
                }
                // 7) UNKNOWN
                else {
                    logger.warn("< Unknown method: {}", req.method());
                    ctx.write(res).addListener(ChannelFutureListener.CLOSE);
                }
            }
        } catch (Exception e) {
            logger.warn("| ({}) Fail to handle UDP Packet.", name, e);
        }
    }

    private void sendResponse(ChannelHandlerContext ctx, DefaultHttpRequest req, FullHttpResponse res) {
        final String cSeq = req.headers().get(RtspHeaderNames.CSEQ);
        if (cSeq != null) {
            res.headers().add(RtspHeaderNames.CSEQ, cSeq);
        }

        final String session = req.headers().get(RtspHeaderNames.SESSION);
        if (session != null) {
            res.headers().add(RtspHeaderNames.SESSION, session);
        }

        res.headers().set(RtspHeaderNames.CONNECTION, RtspHeaderValues.KEEP_ALIVE);
        ctx.write(res);

        logger.debug("Response: {}", res);
        logger.debug("> Send response. ({})", req.method());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.warn("RtspChannelHandler is inactive.");
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("Exception.RtspChannelHandler. (cause={})", cause.toString());
    }

}
