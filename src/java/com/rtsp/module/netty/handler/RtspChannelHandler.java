package com.rtsp.module.netty.handler;

import com.fsm.module.StateHandler;
import com.rtsp.ffmpeg.FfmpegManager;
import com.rtsp.fsm.RtspEvent;
import com.rtsp.fsm.RtspState;
import com.rtsp.module.RtspManager;
import com.rtsp.module.Streamer;
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
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.rtsp.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
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

    public static final int MP2T_TYPE = 33; //RTP payload type for MJPEG video
    public static final String MP2T_TAG = "MP2T"; //RTP payload tag for MJPEG video

    private final String name;

    private final String listenIp; // local ip
    private final int listenPort; // local(listen) port

    private final Random random = new Random();
    private final RtpPacket _rtpPacket = new RtpPacket();

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
                    sendResponse(name, rtspUnit, null, ctx, req, res);
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
                                    "s="+ rtspUnit.getRtspId() + "\r\n" +
                                    "c=IN IP4 " + listenIp + "\r\n" +
                                    "t=0 0\r\n" +
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
                    sendResponse(name, rtspUnit, null, ctx, req, res);


                }
                // 3) SETUP
                else if (req.method() == RtspMethods.SETUP) {
                    logger.debug("({}) ({}) () < SETUP", name, rtspUnit.getRtspId());

                    String transportHeaderContent = req.headers().get("Transport");
                    String clientPortString = transportHeaderContent.substring(
                            transportHeaderContent.lastIndexOf(";") + 1
                    );

                    //
                    String curSessionId = req.headers().get(RtspHeaderNames.SESSION);
                    if (curSessionId == null) {
                        curSessionId = String.valueOf(random.nextInt(Integer.MAX_VALUE));
                    }
                    logger.debug("({}) ({}) () Current sessionId is [{}]. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), curSessionId, listenIp, listenPort);
                    //

                    //
                    Streamer streamer = NettyChannelManager.getInstance().getStreamer(curSessionId, listenIp, listenPort);
                    if (streamer == null) {
                        streamer = rtspUnit.getStreamer();
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
                            StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
                            rtspStateHandler.fire(
                                    RtspEvent.SETUP,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        }
                    }
                    //

                    //
                    streamer.setUri(req.uri());

                    InetSocketAddress remoteSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                    //streamer.setDestIp(AppInstance.getInstance().getConfigManager().getTargetIp());
                    String remoteIpAddress = remoteSocketAddress.getAddress().getHostAddress();
                    streamer.setDestIp(remoteIpAddress); // Remote IP Address

                    //

                    String sessionId = streamer.getSessionId();
                    int rtcpDestPort = 0;

                    res.headers().add(
                            "Session",
                            sessionId
                    );

                    if (clientPortString.startsWith(String.valueOf(RtspHeaderValues.INTERLEAVED))) {
                        logger.debug("({}) ({}) () < Interleaved {}, clientPortString={}", name, rtspUnit.getRtspId(), req.method(), clientPortString);
                        /*res.headers().add(
                                "Transport",
                                "RTP/AVP;unicast;" + clientPortString + ";ssrc=" + streamer.getSsrc()
                        );*/
                        res.setStatus(RtspResponseStatuses.NOT_ACCEPTABLE);
                        sendResponse(name, rtspUnit, streamer, ctx, req, res);
                        logger.debug("({}) ({}) ({}) Setup to stream the media. (rtpDestIp={}, rtpDestPort={}, rtcpDestPort={})",
                                name, rtspUnit.getRtspId(), streamer.getSessionId(), streamer.getDestIp(), streamer.getDestPort(), streamer.getRtcpDestPort()
                        );
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
                        streamer.setDestPort(rtpDestPort);
                        if (rtcpDestPort > 0) {
                            streamer.setRtcpDestPort(rtcpDestPort);
                        }
                        //

                        int destPort = streamer.getDestPort();
                        if (destPort > 0 && sessionId != null) {
                            res.headers().add(
                                    "Transport",
                                    "RTP/AVP;unicast;client_port=" + destPort + ";ssrc=" + streamer.getSsrc()
                            );

                            res.setStatus(RtspResponseStatuses.OK);
                            sendResponse(name, rtspUnit, streamer, ctx, req, res);

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

                    // CHECK REQUEST
                    String curSessionId = req.headers().get(RtspHeaderNames.SESSION);
                    if (curSessionId == null) {
                        logger.warn("({}) ({}) () SessionId is null. Fail to process PLAY method. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), listenIp, listenPort);
                        res.setStatus(RtspResponseStatuses.NOT_ACCEPTABLE);
                        sendResponse(name, rtspUnit, null, ctx, req, res);
                        return;
                    }
                    logger.debug("({}) ({}) () Current sessionId is [{}]. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), curSessionId, listenIp, listenPort);
                    logger.debug("req.headers(): {}", req.headers());

                    double npt1 = 0;
                    double npt2 = 0;

                    if (req.headers().get(RtspHeaderNames.RANGE) != null) {
                        // npt parsing
                        String nptString = req.headers().get(RtspHeaderNames.RANGE);
                        String nptString1 = nptString.substring(nptString.lastIndexOf("=") + 1, nptString.lastIndexOf("-"));
                        String nptString2 = null;
                        if (!nptString.endsWith("-")) {
                            nptString2 = nptString.substring(nptString.lastIndexOf("-") + 1);
                        }

                        npt1 = Double.parseDouble(nptString1);
                        npt2 = 0;
                        if (nptString2 != null && !nptString2.isEmpty()) {
                            npt2 = Double.parseDouble(nptString2);
                        }
                        //
                    }

                    logger.debug("[< PLAY REQ] RANGE: [{} ~ {}]", npt1, npt2);
                    logger.debug("[< PLAY REQ] URI: {}", req.uri());
                    //

                    // CHECK STREAMER
                    Streamer streamer = NettyChannelManager.getInstance().getStreamer(curSessionId, listenIp, listenPort);
                    if (streamer == null) {
                        logger.warn("({}) ({}) () Streamer is not defined. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), listenIp, listenPort);
                        res.setStatus(RtspResponseStatuses.INTERNAL_SERVER_ERROR);
                        res.headers().add(
                                "Session",
                                curSessionId
                        );
                        sendResponse(name, rtspUnit, null, ctx, req, res);
                        return;
                    }
                    //

                    // CHECK RTSP DESTINATION PORT
                    int destPort = streamer.getDestPort();
                    if (destPort <= 0) {
                        logger.warn("({}) ({}) ({}) Fail to process the PLAY request. Destination port is wrong. (rtspUnit={}, destPort={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), rtspUnit, destPort);
                        return;
                    }
                    //

                    // CHECK RTSP DESTINATION IP
                    logger.debug("({}) ({}) ({}) Start to stream the media. (rtpDestPort={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), destPort);
                    NettyChannelManager.getInstance().startStreaming(
                            streamer.getSessionId(),
                            listenIp,
                            listenPort
                    );

                    VideoStream video = streamer.getVideo();
                    logger.debug("({}) ({}) ({}) resultM3U8FilePath: {}", name, rtspUnit.getRtspId(), streamer.getSessionId(), video.getResultM3U8FilePath());

                    //int curTsIndex = streamer.getCurTsIndex();
                    FfmpegManager ffmpegManager = new FfmpegManager();
                    double mp4Time = ffmpegManager.convertMp4ToM3u8(
                            video.getMp4FileName(),
                            video.getResultM3U8FilePath(),
                            (long) npt1
                    );
                    //streamer.setCurTsIndex(curTsIndex + AppInstance.getInstance().getConfigManager().getHlsTime());
                    //

                    //
                    byte[] m3u8ByteData = Files.readAllBytes(
                            Paths.get(
                                    video.getResultM3U8FilePath()
                            )
                    );

                    if (m3u8ByteData.length == 0) {
                        return;
                    }
                    //

                    //
                    List<MediaSegment> mediaSegmentList;
                    MediaPlaylistParser parser = new MediaPlaylistParser();
                    MediaPlaylist playlist = parser.readPlaylist(Paths.get(video.getResultM3U8FilePath()));
                    if (playlist != null) {
                        String m3u8PathOnly = video.getResultM3U8FilePath();
                        m3u8PathOnly = m3u8PathOnly.substring(
                                0,
                                m3u8PathOnly.lastIndexOf("/")
                        );
                        streamer.setM3u8PathOnly(m3u8PathOnly);
                        mediaSegmentList = playlist.mediaSegments();

                        logger.debug("({}) ({}) ({}) mediaSegmentList: {}", name, rtspUnit.getRtspId(), streamer.getSessionId(), mediaSegmentList);
                        streamer.setMediaSegmentList(mediaSegmentList);

                        StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
                        rtspStateHandler.fire(
                                RtspEvent.PLAY,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );
                    } else {
                        logger.warn("({}) ({}) ({}) Fail to stream the media. (rtpDestPort={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), destPort);
                        sendFailResponse(name, rtspUnit, streamer, ctx, req, res, curSessionId, RtspResponseStatuses.INTERNAL_SERVER_ERROR);
                        return;
                    }

                    if (mediaSegmentList == null || mediaSegmentList.isEmpty()) {
                        logger.warn("({}) ({}) ({}) Media segment list is empty. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), listenIp, listenPort);
                        sendFailResponse(name, rtspUnit, streamer, ctx, req, res, curSessionId, RtspResponseStatuses.INTERNAL_SERVER_ERROR);
                        return;
                    }

                    if (!streamer.isActive()) {
                        logger.warn("({}) ({}) ({}) Streamer is not active or deleted. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), listenIp, listenPort);
                        sendFailResponse(name, rtspUnit, streamer, ctx, req, res, curSessionId, RtspResponseStatuses.INTERNAL_SERVER_ERROR);
                        return;
                    }

                    if (npt2 > mp4Time || npt2 < 0) {
                        logger.warn("({}) ({}) ({}) Wrong end time is detected. (listenIp={}, listenPort={})", name, rtspUnit.getRtspId(), streamer.getSessionId(), listenIp, listenPort);
                        sendFailResponse(name, rtspUnit, streamer, ctx, req, res, curSessionId, RtspResponseStatuses.NOT_ACCEPTABLE);
                        return;
                    }

                    // SUCCESS RESPONSE
                    res.setStatus(RtspResponseStatuses.OK);
                    if (npt2 == 0) {
                        res.headers().add(
                                "Range",
                                "npt=" + npt1 + "-" + mp4Time
                        );
                    } else {
                        res.headers().add(
                                "Range",
                                "npt=" + npt1 + "-" + npt2
                        );
                    }
                    res.headers().add(
                            RtspManager.RTSP_RES_SESSION,
                            curSessionId
                    );
                    res.headers().add(
                            "RTP-Info",
                            "url=" + req.uri() + "/trackID=1;seq=" + streamer.getCurSeqNum() + ";rtptime=" + streamer.getCurTimeStamp()
                    );
                    res.headers().add(
                            "Cache-Control",
                            "no-cache"
                    );
                    RtspChannelHandler.sendResponse(name, rtspUnit, streamer, ctx, req, res);
                    //

                    // SEND M3U8
                    ByteBuf buf = Unpooled.copiedBuffer(m3u8ByteData);
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

                    // SEND TS FILES
                    mediaSegmentList = streamer.getMediaSegmentList();
                    String m3u8PathOnly = streamer.getM3u8PathOnly();

                    for (MediaSegment mediaSegment : mediaSegmentList) {
                        logger.debug("({}) ({}) ({}) Current MediaSegment: {}", name, rtspUnit.getRtspId(), streamer.getSessionId(), mediaSegment);
                        String tsFileName = mediaSegment.uri();
                        tsFileName = m3u8PathOnly + File.separator + tsFileName;
                        logger.debug("({}) ({}) ({}) Selected tsFileName: {}", name, rtspUnit.getRtspId(), streamer.getSessionId(), tsFileName);

                        byte[] tsByteData = Files.readAllBytes(Paths.get(tsFileName));
                        logger.debug("[START] [{}] : [{}]", tsFileName, tsByteData.length);

                        int partitionValue = 188; // TS Packet Total byte : 188 (4(header) + 184(PES, Packetized Elementary Streams))
                        int totalLength = tsByteData.length;

                        if (partitionValue > totalLength) {
                            partitionValue = totalLength;
                        }

                        int curByteSize = 0;
                        for (int i = 0; i < totalLength; i += partitionValue) {
                            int curDataLength;
                            if ((i + partitionValue) >= totalLength) {
                                curDataLength = totalLength - i;
                            } else {
                                curDataLength = partitionValue;
                            }

                            byte[] curData = new byte[curDataLength];
                            System.arraycopy(tsByteData, i, curData, 0, curDataLength);

                            int _curSeqNum = streamer.getCurSeqNum();
                            int _curTimeStamp = streamer.getCurTimeStamp();
                            _rtpPacket.setValue(
                                    2, 0, 0, 0, 0, RtspChannelHandler.MP2T_TYPE,
                                    _curSeqNum, _curTimeStamp, streamer.getSsrc(), curData, curData.length
                            );

                            byte[] _totalRtpData = _rtpPacket.getData();
                            ByteBuf _buf = Unpooled.copiedBuffer(_totalRtpData);
                            streamer.send(
                                    _buf,
                                    streamer.getDestIp(),
                                    streamer.getDestPort()
                            );

                                /*logger.debug("({}) ({}) ({}) >> Send TS RTP [{}] (destIp={}, destPort={}, size={})",
                                        name, rtspUnit.getRtspId(), streamer.getSessionId(), _rtpPacket, streamer.getDestIp(), streamer.getDestPort(), _totalRtpData.length
                                );*/

                            streamer.setCurSeqNum(_curSeqNum + 1);
                            streamer.setCurTimeStamp(_curTimeStamp + 100);

                            curByteSize += curDataLength;
                        }

                        logger.debug("[END] [{}] : [sendByteSize: {}] [lastSeqNum: {}] [lastTimeStamp: {}]",
                                tsFileName, curByteSize, streamer.getCurSeqNum(), streamer.getCurTimeStamp()
                        );
                    }
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
                    //

                    //
                    StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
                    rtspStateHandler.fire(
                            RtspEvent.TEARDOWN,
                            rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                    );
                    //

                    //
                    logger.debug("({}) ({}) ({}) Stop the streaming.", name, rtspUnit.getRtspId(), streamer.getSessionId());
                    NettyChannelManager.getInstance().stopStreaming(
                            streamer.getSessionId(),
                            listenIp,
                            listenPort
                    );

                    NettyChannelManager.getInstance().deleteStreamer(
                            streamer.getSessionId(),
                            listenIp,
                            listenPort
                    );
                    rtspUnit.setStreamer(null);
                    logger.debug("({}) ({}) ({}) Finish to stream the media. All media segment is played.", name, rtspUnit.getRtspId(), streamer.getSessionId());
                    //

                    res.setStatus(RtspResponseStatuses.OK);
                    res.headers().add(
                            "Session",
                            curSessionId
                    );
                    sendResponse(name, rtspUnit, streamer, ctx, req, res);
                }
                // 6) PAUSE
                else if (req.method() == RtspMethods.PAUSE) {
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
                        NettyChannelManager.getInstance().pauseStreaming(
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
                    sendResponse(name, rtspUnit, streamer, ctx, req, res);
                }
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

    public static void sendResponse(String name, RtspUnit rtspUnit, Streamer streamer, ChannelHandlerContext ctx, DefaultHttpRequest req, FullHttpResponse res) {
        if (rtspUnit == null) {
            logger.warn("({}) ({}) () Fail to send response. RtspUnit or Streamer is null. (rtspUnit={}, streamer={})", name, req.method(), rtspUnit, streamer);
            return;
        }

        final String cSeq = req.headers().get("Cseq");
        if (cSeq != null) {
            res.headers().add("Cseq", cSeq);
        }

        //res.headers().set(RtspHeaderNames.CONNECTION, RtspHeaderValues.KEEP_ALIVE);
        ctx.write(res);

        String sessionId = null;
        if (streamer != null) {
            sessionId = streamer.getSessionId();
        }

        logger.debug("({}) ({}) ({}) Response: {}", name, rtspUnit.getRtspId(), sessionId, res);
        logger.debug("({}) ({}) ({}) > Send response. ({})", name, rtspUnit.getRtspId(), sessionId, req.method());
    }

    public void sendFailResponse(String name, RtspUnit rtspUnit, Streamer streamer, ChannelHandlerContext ctx, DefaultHttpRequest req, FullHttpResponse res, String curSessionId, HttpResponseStatus httpResponseStatus) {
        res.setStatus(httpResponseStatus);
        res.headers().add(
                "Session",
                curSessionId
        );
        sendResponse(name, rtspUnit, streamer, ctx, req, res);
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
