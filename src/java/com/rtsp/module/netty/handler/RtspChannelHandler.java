package com.rtsp.module.netty.handler;

import com.fsm.StateManager;
import com.fsm.module.StateHandler;
import com.rtsp.ffmpeg.FfmpegManager;
import com.rtsp.fsm.RtspEvent;
import com.rtsp.fsm.RtspState;
import com.rtsp.module.Streamer;
import com.rtsp.module.RtspManager;
import com.rtsp.module.VideoStream;
import com.rtsp.module.base.RtspUnit;
import com.rtsp.module.netty.NettyChannelManager;
import com.rtsp.protocol.RtpPacket;
import com.rtsp.service.ResourceManager;
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
 * HTTP 는 TCP 연결이므로 매번 연결 상태가 변경된다. (연결 생성 > 비즈니스 로직 처리 > 연결 해제)
 */
public class RtspChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RtspChannelHandler.class);

    private static final int MP2T_TYPE = 33; //RTP payload type for MJPEG video
    private static final String MP2T_TAG = "MP2T"; //RTP payload tag for MJPEG video

    private final String name;

    private final String listenIp; // local ip
    private final int listenPort; // local(listen) port

    private final Random random = new Random();

    ////////////////////////////////////////////////////////////////////////////////

    public RtspChannelHandler(String listenIp, int listenPort) {
        this.name = random.nextInt(65536) + "_" + listenIp + ":" + listenPort;

        this.listenIp = listenIp;
        this.listenPort = listenPort;

        logger.debug("({}) RtspChannelHandler is created. (listenIp={}, listenPort={})", name, listenIp, listenPort);
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
                logger.warn("({}) Fail to get the rtsp unit. RtspUnit is null.", name);
                return;
            }

            if (msg instanceof DefaultHttpRequest) {
                DefaultHttpRequest req = (DefaultHttpRequest) msg;
                FullHttpResponse res = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0,  RtspResponseStatuses.NOT_FOUND);

                logger.debug("({}) ({}) () Request: {}", name, rtspUnit.getRtspId(), req);

                // 1) OPTIONS
                if (req.method() == RtspMethods.OPTIONS) {
                    logger.debug("({}) ({}) () < OPTIONS", name, rtspUnit.getRtspId());

                    res.setStatus(RtspResponseStatuses.OK);
                    res.headers().add(
                            RtspHeaderValues.PUBLIC,
                            "OPTIONS, DESCRIBE, SETUP, PLAY, TEARDOWN" // PAUSE
                    );
                    sendResponse(rtspUnit, null, ctx, req, res);
                }
                // 2) DESCRIBE
                else if (req.method() == RtspMethods.DESCRIBE) {
                    logger.debug("({}) ({}) () < DESCRIBE", name, rtspUnit.getRtspId());

                    // Set port to client
                    int port = rtspUnit.getClientPort();
                    if (port == 0) {
                        port = ResourceManager.getInstance().takePort();
                        if (port == -1) {
                            logger.warn("({}) ({}) () Fail to process describe method. Port is full.", name, rtspUnit.getRtspId());
                            return;
                        }
                    }

                    ByteBuf buf = Unpooled.copiedBuffer(
                            "v=0\r\n" +
                                    "c=IN IP4 " + listenIp + "\r\n" +
                                    "t=0\r\n" +
                                    "m=video " + port + " RTP/AVP " + MP2T_TYPE + "\r\n" +
                                    "a=rtpmap:" + MP2T_TYPE + " " + MP2T_TAG + "/90000\r\n",
                            CharsetUtil.UTF_8

                    );
                    logger.debug("({}) ({}) SDP\n{}", name, rtspUnit.getRtspId(), buf.toString(CharsetUtil.UTF_8));

                    if (port > 0) {
                        rtspUnit.setClientPort(port);
                    }

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
                    sendResponse(rtspUnit, null, ctx, req, res);
                }
                // 3) SETUP
                else if (req.method() == RtspMethods.SETUP) {
                    logger.debug("({}) ({}) () < SETUP", name, rtspUnit.getRtspId());

                    String transportHeaderContent = req.headers().get("Transport");
                    String clientPortString = transportHeaderContent.substring(
                            transportHeaderContent.lastIndexOf(";") + 1
                    );

                    int rtcpDestPort = 0;
                    if (clientPortString.startsWith(String.valueOf(RtspHeaderValues.INTERLEAVED))) { // Interleaved 미지원
                        // Must send the packet by TCP, not UDP
                        logger.warn("({}) ({}) () < Unknown method: {}", name, rtspUnit.getRtspId(), req.method());
                        ctx.write(res).addListener(ChannelFutureListener.CLOSE);
                    } else if (clientPortString.startsWith(String.valueOf(RtspHeaderValues.CLIENT_PORT))) {
                        //
                        String rtpDesPortString = clientPortString.substring(
                                clientPortString.lastIndexOf("=") + 1
                        );

                        if (rtpDesPortString.contains("-")) {
                            String rtcpDesPortString = rtpDesPortString.substring(
                                    rtpDesPortString.lastIndexOf("-") + 1
                            );
                            rtcpDestPort = Integer.parseInt(rtcpDesPortString);
                            if (rtcpDestPort <= 0) {
                                logger.warn("({}) ({}) () Fail to parse rtcp destination port. (transportHeaderContent={})", name, rtspUnit.getRtspId(), transportHeaderContent);
                                return;
                            }

                            rtpDesPortString = rtpDesPortString.substring(
                                    0,
                                    rtpDesPortString.lastIndexOf("-")
                            );
                        }

                        int rtpDestPort = Integer.parseInt(rtpDesPortString);
                        if (rtpDestPort <= 0) {
                            logger.warn("({}) ({}) () Fail to parse rtp destination port. (transportHeaderContent={})", name, rtspUnit.getRtspId(), transportHeaderContent);
                            return;
                        }
                        //

                        //
                        String curSessionId = req.headers().get(RtspHeaderNames.SESSION);
                        if (curSessionId == null) {
                            curSessionId = String.valueOf(random.nextInt(Integer.MAX_VALUE));
                        }
                        logger.debug("({}) ({}) () Current sessionId is [{}]. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), curSessionId, listenIp, listenPort);

                        Streamer streamer = NettyChannelManager.getInstance().getStreamer(curSessionId, listenIp, listenPort);
                        if (streamer == null) {
                            streamer = rtspUnit.getStreamer(); // TODO : 임시 조치 (Session 이 아직 구분이 안됨) > 한 번에 하나의 Streaming 밖에 실행 못함
                            if (streamer == null) {
                                streamer = NettyChannelManager.getInstance().addStreamer(
                                        curSessionId,
                                        listenIp,
                                        listenPort
                                );

                                if (streamer == null) {
                                    logger.warn("({}) ({}) ({}) Streamer is not defined. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), curSessionId, listenIp, listenPort);
                                    return;
                                }

                                rtspUnit.setStreamer(streamer);
                                StateHandler rtspStateHandler = StateManager.getInstance().getStateHandler(RtspState.NAME);
                                rtspStateHandler.fire(
                                        RtspEvent.SETUP,
                                        StateManager.getInstance().getStateUnit(streamer.getRtspStateUnitId())
                                );
                            }
                        }
                        //

                        //
                        streamer.setUri(req.uri());
                        streamer.setDestIp(listenIp);
                        streamer.setDestPort(rtpDestPort);
                        if (rtcpDestPort > 0) {
                            streamer.setRtcpDestPort(rtcpDestPort);
                        }
                        //

                        String sessionId = streamer.getSessionId();
                        int destPort = streamer.getDestPort();
                        if (destPort > 0 && sessionId != null) {
                            //
                            res.headers().add(
                                    "Session",
                                    sessionId
                            );

                            res.headers().add(
                                    "Transport",
                                    "RTP/AVP;unicast;client_port=" + destPort + ";ssrc=" + streamer.getSsrc()
                            );

                            res.setStatus(RtspResponseStatuses.OK);
                            sendResponse(rtspUnit, streamer, ctx, req, res);
                            //

                            logger.debug("({}) ({}) ({}) Setup to stream the media. (rtpDestIp={}, rtpDestPort={}, rtcpDestPort={})",
                                    name, rtspUnit.getRtspId(), streamer.getSessionId(), streamer.getDestIp(), streamer.getDestPort(), streamer.getRtcpDestPort()
                            );
                        } else {
                            logger.warn("({}) ({}) ({}) Fail to send the response for SETUP. (rtpDestPort={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), streamer.getDestPort());
                        }
                    } else {
                        logger.warn("({}) ({}) () Unknown transport header content. ({})", name, rtspUnit.getRtspId(), clientPortString);
                    }
                }
                // 4) PLAY
                else if (req.method() == RtspMethods.PLAY) {
                    logger.debug("({}) ({}) () < PLAY", name, rtspUnit.getRtspId());

                    //
                    String curSessionId = req.headers().get(RtspHeaderNames.SESSION);
                    if (curSessionId == null) {
                        logger.warn("({}) ({}) () SessionId is null. Fail to process PLAY method. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), listenIp, listenPort);
                        return;
                    }
                    logger.debug("({}) ({}) () Current sessionId is [{}]. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), curSessionId, listenIp, listenPort);

                    Streamer streamer = NettyChannelManager.getInstance().getStreamer(curSessionId, listenIp, listenPort);
                    if (streamer == null) {
                        logger.warn("({}) ({}) () Streamer is not defined. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), listenIp, listenPort);
                        return;
                    }

                    int destPort = streamer.getDestPort();
                    if (destPort <= 0) {
                        logger.warn("({}) ({}) ({}) Fail to process the PLAY request. Destination port is wrong. (rtspUnit={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), rtspUnit);
                        return;
                    }

                    if (!streamer.isMediaEnabled()) {
                        StateHandler rtspStateHandler = StateManager.getInstance().getStateHandler(RtspState.NAME);
                        rtspStateHandler.fire(
                                RtspEvent.PLAY,
                                StateManager.getInstance().getStateUnit(streamer.getRtspStateUnitId())
                        );

                        logger.debug("({}) ({}) ({}) Start to stream the media. (rtpDestPort={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), destPort);
                        NettyChannelManager.getInstance().startStreaming(
                                streamer.getSessionId(),
                                listenIp,
                                listenPort
                        );

                        if (!streamer.isActive()) {
                            logger.warn("({}) ({}) ({}) Streamer is not active or deleted. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), listenIp, listenPort);
                            return;
                        }

                        VideoStream video = streamer.getVideo();
                        logger.debug("({}) ({}) ({}) resultM3U8FilePath: {}", name, rtspUnit.getRtspId(), streamer.getSessionId(), video.getResultM3U8FilePath());

                        FfmpegManager ffmpegManager = new FfmpegManager();
                        ffmpegManager.convertMp4ToM3u8(
                                video.getMp4FileName(),
                                video.getResultM3U8FilePath()
                        );

                        byte[] m3u8ByteData = Files.readAllBytes(
                                Paths.get(
                                        video.getResultM3U8FilePath()
                                )
                        );

                        //
                        int curSeqNum = streamer.getCurSeqNum();
                        int curTimeStamp = streamer.getCurTimeStamp();
                        RtpPacket rtpPacket = new RtpPacket();
                        rtpPacket.setValue(
                                2, 0, 0, 0, 0, MP2T_TYPE, curSeqNum,
                                curTimeStamp,
                                streamer.getSsrc(),
                                m3u8ByteData,
                                m3u8ByteData.length
                        );
                        streamer.setCurSeqNum(curSeqNum + 1);
                        streamer.setCurTimeStamp(curTimeStamp + 100);

                        byte[] totalRtpData = rtpPacket.getData();
                        ByteBuf buf = Unpooled.copiedBuffer(totalRtpData);
                        streamer.send(
                                buf,
                                streamer.getDestIp(),
                                streamer.getDestPort()
                        );
                        logger.debug("({}) ({}) ({}) >> Send M3U8 (destIp={}, destPort={})\n{}(size={})",
                                name, rtspUnit.getRtspId(),
                                streamer.getSessionId(), streamer.getDestIp(), streamer.getDestPort(),
                                new String(m3u8ByteData, StandardCharsets.UTF_8), m3u8ByteData.length
                        );
                        //

                        //
                        MediaPlaylistParser parser = new MediaPlaylistParser();
                        MediaPlaylist playlist = parser.readPlaylist(Paths.get(video.getResultM3U8FilePath()));
                        if (playlist != null) {
                            String m3u8PathOnly = video.getResultM3U8FilePath();
                            m3u8PathOnly = m3u8PathOnly.substring(
                                    0,
                                    m3u8PathOnly.lastIndexOf("/")
                            );
                            streamer.setM3u8PathOnly(m3u8PathOnly);

                            List<MediaSegment> mediaSegmentList = playlist.mediaSegments();
                            logger.debug("({}) ({}) ({}) mediaSegmentList: {}", name, rtspUnit.getRtspId(), streamer.getSessionId(), mediaSegmentList);
                            streamer.setMediaSegmentList(mediaSegmentList);
                        }
                        streamer.setMediaEnabled(true);
                        //
                    } else {
                        logger.debug("({}) ({}) ({}) Start to stream the media. (rtpDestPort={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), destPort);
                        NettyChannelManager.getInstance().startStreaming(
                                streamer.getSessionId(),
                                listenIp,
                                listenPort
                        );

                        if (!streamer.isActive()) {
                            logger.warn("({}) ({}) ({}) Streamer is not active or deleted. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), listenIp, listenPort);
                            return;
                        }

                        List<MediaSegment> mediaSegmentList = streamer.getMediaSegmentList();
                        String m3u8PathOnly = streamer.getM3u8PathOnly();

                        logger.debug("({}) ({}) ({}) Streaming...", name, rtspUnit.getRtspId(), streamer.getSessionId());
                        for (MediaSegment mediaSegment : mediaSegmentList) {
                            String tsFileName = mediaSegment.uri();
                            tsFileName = m3u8PathOnly + File.separator + tsFileName;
                            String curTsFileName = RtspUnit.getPureFileName(streamer.getUri());
                            if (tsFileName.trim().equals(curTsFileName.trim())) {
                                logger.debug("({}) ({}) ({}) Selected tsFileName: {}", name, rtspUnit.getRtspId(), streamer.getSessionId(), tsFileName);

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

                                    int curSeqNum = streamer.getCurSeqNum();
                                    int curTimeStamp = streamer.getCurTimeStamp();
                                    RtpPacket rtpPacket = new RtpPacket();
                                    rtpPacket.setValue(
                                            2, 0, 0, 0, marker, MP2T_TYPE, curSeqNum,
                                            curTimeStamp,
                                            streamer.getSsrc(),
                                            curData,
                                            curData.length
                                    );
                                    streamer.setCurSeqNum(curSeqNum + 1);
                                    streamer.setCurTimeStamp(curTimeStamp + 100);

                                    byte[] totalRtpData = rtpPacket.getData();
                                    ByteBuf buf = Unpooled.copiedBuffer(totalRtpData);
                                    streamer.send(
                                            buf,
                                            streamer.getDestIp(),
                                            streamer.getDestPort()
                                    );
                                    logger.trace("({}) ({}) ({}) >> Send TS (destIp={}, destPort={}, size={})",
                                            name, rtspUnit.getRtspId(), streamer.getSessionId(), streamer.getDestIp(), streamer.getDestPort(), totalRtpData.length
                                    );

                                    if ((i + partitionValue) >= totalLength) {
                                        marker = 1;
                                    }
                                }

                                break;
                            }
                        }

                        streamer.setCurMediaSegmentCount(
                                streamer.getCurMediaSegmentCount() + 1
                        );
                    }
                    //

                    if (NettyChannelManager.getInstance().getStreamer(streamer.getSessionId(), listenIp, listenPort) != null) {
                        NettyChannelManager.getInstance().stopStreaming(
                                streamer.getSessionId(),
                                listenIp,
                                listenPort
                        );
                        logger.debug("({}) ({}) ({}) Stop to stream the media. (rtpDestPort={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), destPort);
                    }

                    res.setStatus(RtspResponseStatuses.OK);
                    res.headers().add(
                            "Session",
                            curSessionId
                    );
                    sendResponse(rtspUnit, streamer, ctx, req, res);
                }
                // 5) TEARDOWN
                else if (req.method() == RtspMethods.TEARDOWN) {
                    logger.debug("({}) ({}) () < TEARDOWN", name, rtspUnit.getRtspId());

                    //
                    String curSessionId = req.headers().get(RtspHeaderNames.SESSION);
                    if (curSessionId == null) {
                        logger.warn("({}) ({}) () SessionId is null. Fail to process TEARDOWN method. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), listenIp, listenPort);
                        return;
                    }
                    logger.debug("({}) ({}) () Current sessionId is [{}]. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), curSessionId, listenIp, listenPort);

                    Streamer streamer = NettyChannelManager.getInstance().getStreamer(curSessionId, listenIp, listenPort);
                    if (streamer == null) {
                        logger.warn("({}) ({}) () Streamer is not defined. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), listenIp, listenPort);
                        return;
                    }

                    StateHandler rtspStateHandler = StateManager.getInstance().getStateHandler(RtspState.NAME);
                    rtspStateHandler.fire(
                            RtspEvent.TEARDOWN,
                            StateManager.getInstance().getStateUnit(streamer.getRtspStateUnitId())
                    );

                    // 마지막 TEARDOWN 수신 시 해당 세션의 모든 미디어 스트리밍 자원 삭제
                    if (streamer.getCurMediaSegmentCount() == streamer.getMediaSegmentList().size()) {
                        NettyChannelManager.getInstance().deleteStreamer(
                                streamer.getSessionId(),
                                listenIp,
                                listenPort
                        );
                        rtspUnit.setStreamer(null); // TODO : 임시 조치
                        logger.debug("({}) ({}) ({}) Finish to stream the media. All media segment is played.", name, rtspUnit.getRtspId(), streamer.getSessionId());
                    } else {
                        logger.debug("({}) ({}) ({}) Stop the streaming.)", name, rtspUnit.getRtspId(), streamer.getSessionId());
                        NettyChannelManager.getInstance().stopStreaming(
                                streamer.getSessionId(),
                                listenIp,
                                listenPort
                        );
                    }
                    //

                    res.setStatus(RtspResponseStatuses.OK);
                    res.headers().add(
                            "Session",
                            curSessionId
                    );
                    sendResponse(rtspUnit, streamer, ctx, req, res);
                }
                // 6) PAUSE > TODO : Not implemented
                /*else if (req.method() == RtspMethods.PAUSE) {
                    logger.debug("({}) ({}) () < PAUSE", name, rtspUnit.getRtspId());

                    //
                    String curSessionId = req.headers().get(RtspHeaderNames.SESSION);
                    if (curSessionId == null) {
                        logger.warn("({}) ({}) () SessionId is null. Fail to process PAUSE method. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), listenIp, listenPort);
                        return;
                    }
                    logger.debug("({}) ({}) () Current sessionId is [{}]. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), curSessionId, listenIp, listenPort);

                    Streamer streamer = NettyChannelManager.getInstance().getStreamer(curSessionId, listenIp, listenPort);
                    if (streamer == null) {
                        logger.warn("({}) ({}) () Streamer is not defined. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), listenIp, listenPort);
                        return;
                    }

                    if (NettyChannelManager.getInstance().getStreamer(streamer.getSessionId(), listenIp, listenPort) != null) {
                        logger.debug("({}) ({}) ({}) Stop the streaming. (rtpDestPort={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), streamer.getDestPort());
                        NettyChannelManager.getInstance().stopStreaming(
                                streamer.getSessionId(),
                                listenIp,
                                listenPort
                        );
                    }
                    //

                    res.setStatus(RtspResponseStatuses.OK);
                    res.headers().add(
                            "Session",
                            curSessionId
                    );
                    sendResponse(rtspUnit, streamer, ctx, req, res);
                }*/
                // 7) UNKNOWN
                else {
                    logger.warn("({}) ({}) () < Unknown method: {}", name, rtspUnit.getRtspId(), req.method());
                    ctx.write(res).addListener(ChannelFutureListener.CLOSE);
                }
            }
        } catch (Exception e) {
            logger.warn("({}) ({}) Fail to handle UDP Packet.", name, e);
        }
    }

    private void sendResponse(RtspUnit rtspUnit, Streamer streamer, ChannelHandlerContext ctx, DefaultHttpRequest req, FullHttpResponse res) {
        final String cSeq = req.headers().get("Cseq");
        if (cSeq != null) {
            res.headers().add("Cseq", cSeq);
        }

        res.headers().set(RtspHeaderNames.CONNECTION, RtspHeaderValues.KEEP_ALIVE);
        ctx.write(res);

        String sessionId = null;
        if (streamer != null) {
            sessionId = streamer.getSessionId();
        }

        logger.debug("({}) ({}) ({}) Response: {}", name, rtspUnit.getRtspId(), sessionId, res);
        logger.debug("({}) ({}) ({}) > Send response. ({})", name, rtspUnit.getRtspId(), sessionId, req.method());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.warn("({}) RtspChannelHandler is inactive.", name);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("({}) RtspChannelHandler.Exception (cause={})", name, cause.toString());
    }

}
