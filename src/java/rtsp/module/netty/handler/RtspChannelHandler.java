package rtsp.module.netty.handler;

import com.fsm.module.StateHandler;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.ffmpeg.FfmpegManager;
import rtsp.fsm.RtspEvent;
import rtsp.fsm.RtspState;
import rtsp.module.RtspManager;
import rtsp.module.Streamer;
import rtsp.module.VideoStream;
import rtsp.module.base.RtspUnit;
import rtsp.module.netty.NettyChannelManager;
import rtsp.module.sdp.base.Sdp;
import rtsp.service.AppInstance;
import rtsp.service.ResourceManager;
import rtsp.service.ServiceManager;
import rtsp.service.scheduler.schedule.ScheduleManager;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Random;
import java.util.concurrent.TimeUnit;

/**
 * @class public class RtspChannelHandler extends ChannelInboundHandlerAdapter
 * @brief RtspChannelHandler class
 * HTTP 는 TCP 연결이므로 매번 연결 상태가 변경된다. (연결 생성 > 비즈니스 로직 처리 > 연결 해제)
 */
public class RtspChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RtspChannelHandler.class);

    private final String name;
    private final String rtspUnitId;

    private final String listenIp; // local ip
    private final int listenRtspPort; // local(listen) rtsp port
    private final int listenRtcpPort; // local(listen) rtcp port

    private final Random random = new Random();

    ////////////////////////////////////////////////////////////////////////////////

    public RtspChannelHandler(String rtspUnitId, String listenIp, int listenRtspPort, int listenRtcpPort) {
        this.name = "RTSP_" + rtspUnitId + "_" + listenIp + ":" + listenRtspPort;

        this.rtspUnitId = rtspUnitId;
        this.listenIp = listenIp;
        this.listenRtspPort = listenRtspPort;
        this.listenRtcpPort = listenRtcpPort;

        logger.debug("({}) RtspChannelHandler is created. (listenIp={}, listenRtspPort={}, listenRtcpPort={})", name, listenIp, listenRtspPort, listenRtcpPort);
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg instanceof DefaultHttpRequest) {
                ///////////////////////////////////////////////////////////////////////////
                // HTTP REQUEST PARSING
                DefaultHttpRequest req = (DefaultHttpRequest) msg;
                DefaultFullHttpResponse res = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0,  RtspResponseStatuses.NOT_FOUND);
                if (req.decoderResult().isFailure()) {
                    logger.warn("({}) Fail to process the request. Bad request is detected.", name);
                    sendFailResponse(name, null, null, ctx, req, res, null, RtspResponseStatuses.BAD_REQUEST);
                    return;
                }
                ///////////////////////////////////////////////////////////////////////////

                ///////////////////////////////////////////////////////////////////////////
                // CHECK RTSP UNIT
                RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit(rtspUnitId);
                if (rtspUnit == null) {
                    logger.warn("({}) Fail to get the rtsp unit. RtspUnit is null.", name);
                    sendFailResponse(name, null, null, ctx, req, res, null, RtspResponseStatuses.INTERNAL_SERVER_ERROR);
                    return;
                }
                StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
                String curState = rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId()).getCurState();
                logger.debug("({}) ({}) () Request: {}", name, rtspUnit.getRtspUnitId(), req);
                ///////////////////////////////////////////////////////////////////////////

                ///////////////////////////////////////////////////////////////////////////
                // 1) OPTIONS
                if (req.method() == RtspMethods.OPTIONS) {
                    logger.debug("({}) ({}) () < OPTIONS (curState={})", name, rtspUnit.getRtspUnitId(), curState);

                    if (curState.equals(RtspState.REGISTER) || curState.equals(RtspState.PLAY) || curState.equals(RtspState.PAUSE)) {
                        String sessionId = req.headers().get(RtspHeaderNames.SESSION);
                        if (sessionId != null) {
                            long prevSessionId = rtspUnit.getSessionId();
                            rtspUnit.setSessionId(Long.parseLong(sessionId));
                            logger.debug("({}) ({}) () Previous SessionId is [{}]. SessionId is updated. ({})", name, rtspUnit.getRtspUnitId(), prevSessionId, sessionId);
                        } else {
                            rtspStateHandler.fire(
                                    RtspEvent.OPTIONS,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );

                            long newSessionId = random.nextInt(1000000);
                            logger.warn("({}) ({}) () SessionId is null. New sessionId is created. ({})", name, rtspUnit.getRtspUnitId(), newSessionId);
                            rtspUnit.setSessionId(newSessionId);
                        }

                        res.setStatus(RtspResponseStatuses.OK);
                        res.headers().add(
                                RtspHeaderValues.PUBLIC,
                                RtspMethods.OPTIONS + ", " +
                                        RtspMethods.DESCRIBE + ", " +
                                        RtspMethods.SETUP + ", " +
                                        RtspMethods.PLAY + ", " +
                                        RtspMethods.PAUSE + ", " +
                                        RtspMethods.TEARDOWN
                        );
                        sendResponse(name, rtspUnit, null, ctx, req, res);
                    }
                }
                ///////////////////////////////////////////////////////////////////////////

                ///////////////////////////////////////////////////////////////////////////
                // 2) DESCRIBE
                else if (req.method() == RtspMethods.DESCRIBE) {
                    logger.debug("({}) ({}) () < DESCRIBE", name, rtspUnit.getRtspUnitId());

                    if (curState.equals(RtspState.OPTIONS)) {
                        rtspStateHandler.fire(
                                RtspEvent.DESCRIBE,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );

                        // Set port to client
                        int clientRtpListenPort = rtspUnit.getClientRtpListenPort();
                        if (clientRtpListenPort == 0) {
                            clientRtpListenPort = ResourceManager.getInstance().takePort();
                            if (clientRtpListenPort == -1) {
                                logger.warn("({}) ({}) () Fail to process describe method. Port is full.", name, rtspUnit.getRtspUnitId());
                                rtspStateHandler.fire(
                                        RtspEvent.DESCRIBE_FAIL,
                                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                                );
                                sendFailResponse(name, rtspUnit, null, ctx, req, res, null, RtspResponseStatuses.INTERNAL_SERVER_ERROR);
                                return;
                            }
                        }
                        rtspUnit.setClientRtpListenPort(clientRtpListenPort);

                        Sdp localSdp = AppInstance.getInstance().getConfigManager().loadLocalSdpConfig(rtspUnitId, clientRtpListenPort);
                        rtspUnit.setSdp(localSdp);

                        res.setStatus(RtspResponseStatuses.OK);
                        res.headers().add(
                                RtspHeaderNames.CONTENT_TYPE,
                                "application/sdp"
                        );

                        ByteBuf buf = Unpooled.copiedBuffer(localSdp.getData(true), StandardCharsets.UTF_8);
                        res.headers().add(
                                RtspHeaderNames.CONTENT_LENGTH,
                                buf.writerIndex()
                        );

                        res.content().writeBytes(buf);
                        rtspStateHandler.fire(
                                RtspEvent.DESCRIBE_OK,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );

                        sendResponse(name, rtspUnit, null, ctx, req, res);
                    }
                }
                ///////////////////////////////////////////////////////////////////////////

                ///////////////////////////////////////////////////////////////////////////
                // 3) SETUP
                else if (req.method() == RtspMethods.SETUP) {
                    logger.debug("({}) ({}) () < SETUP", name, rtspUnit.getRtspUnitId());

                    if (curState.equals(RtspState.SDP_READY) || curState.equals(RtspState.OPTIONS)) {
                        rtspStateHandler.fire(
                                RtspEvent.SETUP,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );

                        String transportHeaderContent = req.headers().get(RtspHeaderNames.TRANSPORT);
                        String clientPortString = transportHeaderContent.substring(
                                transportHeaderContent.lastIndexOf(";") + 1
                        );

                        //
                        String curSessionId = String.valueOf(rtspUnit.getSessionId());
                        logger.debug("({}) ({}) () Current sessionId is [{}]. (listenIp={}, listenPort={})", name, rtspUnit.getRtspUnitId(), curSessionId, listenIp, listenRtspPort);
                        //

                        //
                        Streamer streamer = NettyChannelManager.getInstance().getStreamer(rtspUnitId, curSessionId, listenIp, listenRtspPort);
                        if (streamer == null) {
                            streamer = rtspUnit.getStreamer();
                            if (streamer == null) {
                                streamer = NettyChannelManager.getInstance().addStreamer(
                                        rtspUnitId,
                                        curSessionId,
                                        listenIp,
                                        listenRtspPort
                                );

                                if (streamer == null) {
                                    logger.warn("({}) ({}) ({}) Streamer is not defined. (listenIp={}, listenPort={})", name, rtspUnit.getRtspUnitId(), curSessionId, listenIp, listenRtspPort);
                                    rtspStateHandler.fire(
                                            RtspEvent.SETUP_FAIL,
                                            rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                                    );
                                    sendFailResponse(name, rtspUnit, null, ctx, req, res, curSessionId, RtspResponseStatuses.NOT_ACCEPTABLE);
                                    return;
                                }

                                rtspUnit.setStreamer(streamer);
                            }
                        }
                        //

                        //
                        String uri = req.uri();
                        if (uri.contains("*")) {
                            uri = uri.replaceAll("[*]", " ");
                        }
                        streamer.setUri(uri);

                        String userAgent = req.headers().get(RtspHeaderNames.USER_AGENT);
                        streamer.setClientUserAgent(userAgent);

                        InetSocketAddress remoteSocketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                        String remoteIpAddress = remoteSocketAddress.getAddress().getHostAddress();
                        streamer.setDestIp(remoteIpAddress); // Remote IP Address
                        logger.warn("({}) ({}) ({}) Destination ip is [{}].", name, rtspUnit.getRtspUnitId(), curSessionId, remoteIpAddress);
                        //

                        String sessionId = streamer.getSessionId();
                        int rtcpDestPort = 0;

                        res.headers().add(
                                RtspHeaderNames.SESSION,
                                sessionId
                        );

                        if (clientPortString.startsWith(String.valueOf(RtspHeaderValues.INTERLEAVED))) {
                            logger.debug("({}) ({}) () < Interleaved {}, clientPortString={}", name, rtspUnit.getRtspUnitId(), req.method(), clientPortString);
                            logger.debug("({}) ({}) ({}) Fail to setup to stream the media. TCP RTP Connection is detected. Unsupported... (rtpDestIp={}, rtpDestPort={}, rtcpDestPort={})",
                                    name, rtspUnit.getRtspUnitId(), streamer.getSessionId(), streamer.getDestIp(), streamer.getDestPort(), streamer.getRtcpDestPort()
                            );
                            sendFailResponse(name, rtspUnit, null, ctx, req, res, curSessionId, RtspResponseStatuses.NOT_ACCEPTABLE);
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
                                    logger.warn("({}) ({}) () Fail to parse rtcp destination port. (transportHeaderContent={})", name, rtspUnit.getRtspUnitId(), transportHeaderContent);
                                    rtspStateHandler.fire(
                                            RtspEvent.SETUP_FAIL,
                                            rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                                    );
                                    sendFailResponse(name, rtspUnit, streamer, ctx, req, res, curSessionId, RtspResponseStatuses.NOT_ACCEPTABLE);
                                    return;
                                }

                                rtpDesPortString = rtpDesPortString.substring(
                                        0,
                                        rtpDesPortString.lastIndexOf("-")
                                );
                            }

                            int rtpDestPort = Integer.parseInt(rtpDesPortString);
                            if (rtpDestPort <= 0) {
                                logger.warn("({}) ({}) () Fail to parse rtp destination port. (transportHeaderContent={})", name, rtspUnit.getRtspUnitId(), transportHeaderContent);
                                rtspStateHandler.fire(
                                        RtspEvent.SETUP_FAIL,
                                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                                );
                                sendFailResponse(name, rtspUnit, streamer, ctx, req, res, curSessionId, RtspResponseStatuses.NOT_ACCEPTABLE);
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
                                if (streamer.getRtcpDestPort() > 0) {
                                    res.headers().add(
                                            RtspHeaderNames.TRANSPORT,
                                            "RTP/AVP;unicast;client_port=" + destPort + "-" + streamer.getRtcpDestPort()
                                                    + ";server_port=" + listenRtspPort + "-" + listenRtcpPort
                                                    + ";ssrc=" + streamer.getSsrc()
                                    );
                                } else {
                                    res.headers().add(
                                            RtspHeaderNames.TRANSPORT, 
                                            "RTP/AVP;unicast;client_port=" + destPort
                                                    + ";server_port=" + listenRtspPort + "-" + listenRtcpPort
                                                    + ";ssrc=" + streamer.getSsrc()
                                    );
                                }

                                res.setStatus(RtspResponseStatuses.OK);
                                sendResponse(name, rtspUnit, streamer, ctx, req, res);

                                logger.debug("({}) ({}) ({}) Setup to stream the media. (rtpDestIp={}, rtpDestPort={}, rtcpDestPort={})",
                                        name, rtspUnit.getRtspUnitId(), streamer.getSessionId(), streamer.getDestIp(), streamer.getDestPort(), streamer.getRtcpDestPort()
                                );
                            } else {
                                logger.warn("({}) ({}) ({}) Fail to send the response for SETUP. (rtpDestPort={})", name, rtspUnit.getRtspUnitId(), streamer.getSessionId(), streamer.getDestPort());
                                rtspStateHandler.fire(
                                        RtspEvent.SETUP_FAIL,
                                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                                );
                            }
                        } else {
                            logger.warn("({}) ({}) () Unknown transport header content. ({})", name, rtspUnit.getRtspUnitId(), clientPortString);
                            rtspStateHandler.fire(
                                    RtspEvent.SETUP_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                        }
                    }
                }
                ///////////////////////////////////////////////////////////////////////////

                ///////////////////////////////////////////////////////////////////////////
                // 4) PLAY
                else if (req.method() == RtspMethods.PLAY) {
                    logger.debug("({}) ({}) () < PLAY", name, rtspUnit.getRtspUnitId());

                    if (curState.equals(RtspState.SETUP) || curState.equals(RtspState.PAUSE)) {
                        rtspStateHandler.fire(
                                RtspEvent.PLAY,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );

                        ///////////////////////////////////////////////////////////////////////////
                        // CHECK REQUEST
                        String curSessionId = req.headers().get(RtspHeaderNames.SESSION);
                        if (curSessionId == null) {
                            logger.warn("({}) ({}) () SessionId is null. Fail to process PLAY method. (listenIp={}, listenPort={})", name, rtspUnit.getRtspUnitId(), listenIp, listenRtspPort);
                            rtspStateHandler.fire(
                                    RtspEvent.PLAY_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                            sendFailResponse(name, rtspUnit, null, ctx, req, res, null, RtspResponseStatuses.NOT_ACCEPTABLE);
                            return;
                        }
                        logger.debug("({}) ({}) () Current sessionId is [{}]. (listenIp={}, listenPort={})", name, rtspUnit.getRtspUnitId(), curSessionId, listenIp, listenRtspPort);
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // CHECK STREAMER
                        Streamer streamer = NettyChannelManager.getInstance().getStreamer(rtspUnitId, curSessionId, listenIp, listenRtspPort);
                        if (streamer == null) {
                            logger.warn("({}) ({}) ({}) Streamer is not defined. (listenIp={}, listenPort={})", name, rtspUnit.getRtspUnitId(), curSessionId, listenIp, listenRtspPort);
                            rtspStateHandler.fire(
                                    RtspEvent.PLAY_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                            sendFailResponse(name, rtspUnit, null, ctx, req, res, curSessionId, RtspResponseStatuses.INTERNAL_SERVER_ERROR);
                            return;
                        }

                        if (!curSessionId.equals(streamer.getSessionId())) {
                            logger.warn("({}) ({}) ({}) SessionId is unmatched. (listenIp={}, listenPort={})", name, rtspUnit.getRtspUnitId(), curSessionId, listenIp, listenRtspPort);
                            rtspStateHandler.fire(
                                    RtspEvent.PLAY_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                            sendFailResponse(name, rtspUnit, streamer, ctx, req, res, curSessionId, RtspResponseStatuses.NOT_ACCEPTABLE);
                            return;
                        }
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // NPT PARSING
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
                            rtspUnit.setStartTime(npt1);
                            npt2 = 0;
                            if (nptString2 != null && !nptString2.isEmpty()) {
                                npt2 = Double.parseDouble(nptString2);
                                rtspUnit.setEndTime(npt2);
                            }
                            //
                        }
                        logger.debug("[< PLAY REQ] RANGE: [{} ~ {}]", npt1, npt2);
                        logger.debug("[< PLAY REQ] URI: {}", streamer.getUri());
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // CHECK RTSP DESTINATION PORT
                        int destPort = streamer.getDestPort();
                        if (destPort <= 0) {
                            logger.warn("({}) ({}) ({}) Fail to process the PLAY request. Destination port is wrong. (rtspUnit={}, destPort={})", name, rtspUnit.getRtspUnitId(), streamer.getSessionId(), rtspUnit, destPort);
                            rtspStateHandler.fire(
                                    RtspEvent.PLAY_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                            sendFailResponse(name, rtspUnit, streamer, ctx, req, res, curSessionId, RtspResponseStatuses.INTERNAL_SERVER_ERROR);
                            return;
                        }
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // CHECK RTSP DESTINATION IP
                        logger.debug("({}) ({}) ({}) Start to stream the media. (rtpDestPort={})", name, rtspUnit.getRtspUnitId(), streamer.getSessionId(), destPort);
                        NettyChannelManager.getInstance().startStreaming(
                                rtspUnitId,
                                streamer.getSessionId(),
                                listenIp,
                                listenRtspPort
                        );

                        VideoStream video = streamer.getVideo();
                        logger.debug("({}) ({}) ({}) resultM3U8FilePath: {}", name, rtspUnit.getRtspUnitId(), streamer.getSessionId(), video.getResultM3U8FilePath());

                        FfmpegManager ffmpegManager = new FfmpegManager();

                        double fileTime = ffmpegManager.getFileTime(video.getMp4FileName());
                        rtspUnit.setFileTime(fileTime);
                        String fileTimeString = String.format("%.3f", fileTime);

                        if (npt2 > fileTime || npt2 < 0) {
                            logger.warn("({}) ({}) ({}) Wrong end time is detected. (listenIp={}, listenPort={})", name, rtspUnit.getRtspUnitId(), streamer.getSessionId(), listenIp, listenRtspPort);
                            rtspStateHandler.fire(
                                    RtspEvent.PLAY_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                            sendFailResponse(name, rtspUnit, streamer, ctx, req, res, curSessionId, RtspResponseStatuses.NOT_ACCEPTABLE);
                            return;
                        }
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // SUCCESS RESPONSE
                        res.setStatus(RtspResponseStatuses.OK);
                        String npt1TempString = String.format("%.3f", npt1);
                        if (npt2 == 0) {
                            res.headers().add(
                                    RtspHeaderNames.RANGE,
                                    "npt=" + npt1TempString + "-" + fileTimeString
                            );
                        } else {
                            String npt2TempString = String.format("%.3f", npt2);
                            res.headers().add(
                                    RtspHeaderNames.RANGE,
                                    "npt=" + npt1TempString + "-" + npt2TempString
                            );
                        }

                        res.headers().add(
                                RtspHeaderNames.SERVER,
                                "URTSP Server"
                        );
                        res.headers().add(
                                RtspManager.RTSP_RES_SESSION,
                                curSessionId
                        );
                        res.headers().add(
                                RtspHeaderNames.RTP_INFO,
                                "url=" + streamer.getUri() + ";seq=" + streamer.getCurSeqNum() + ";rtptime=" + streamer.getCurTimeStamp()
                        );
                        RtspChannelHandler.sendResponse(name, rtspUnit, streamer, ctx, req, res);
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // RTP SENDER THREAD
                        streamer.setPaused(false);
                        RtpSender rtpSender = new RtpSender(
                                rtspUnitId,
                                0, 0, TimeUnit.MILLISECONDS,
                                1, 1, false,
                                ffmpegManager, video,
                                fileTime, npt1, npt2,
                                rtspStateHandler, rtspUnit, streamer, destPort
                        );
                        ScheduleManager.getInstance().startJob(ServiceManager.MAIN_SCHEDULE_JOB, rtpSender);
                        ///////////////////////////////////////////////////////////////////////////
                    }
                }
                ///////////////////////////////////////////////////////////////////////////

                ///////////////////////////////////////////////////////////////////////////
                // 5) TEARDOWN
                else if (req.method() == RtspMethods.TEARDOWN) {
                    logger.debug("({}) ({}) () < TEARDOWN", name, rtspUnit.getRtspUnitId());

                    if (curState.equals(RtspState.SDP_READY) || curState.equals(RtspState.PLAY) || curState.equals(RtspState.PAUSE)) {
                        rtspStateHandler.fire(
                                RtspEvent.TEARDOWN,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );

                        ///////////////////////////////////////////////////////////////////////////
                        String curSessionId = req.headers().get(RtspHeaderNames.SESSION);
                        if (curSessionId == null) {
                            logger.warn("({}) ({}) () SessionId is null. Fail to process TEARDOWN method. (listenIp={}, listenPort={})", name, rtspUnit.getRtspUnitId(), listenIp, listenRtspPort);
                            sendFailResponse(name, rtspUnit, null, ctx, req, res, null, RtspResponseStatuses.NOT_ACCEPTABLE);
                            return;
                        }
                        logger.debug("({}) ({}) () Current sessionId is [{}]. (listenIp={}, listenPort={})", name, rtspUnit.getRtspUnitId(), curSessionId, listenIp, listenRtspPort);

                        Streamer streamer = NettyChannelManager.getInstance().getStreamer(rtspUnitId, curSessionId, listenIp, listenRtspPort);
                        if (streamer != null) {
                            logger.debug("({}) ({}) ({}) Stop the streaming.", name, rtspUnit.getRtspUnitId(), streamer.getSessionId());
                            NettyChannelManager.getInstance().stopStreaming(
                                    rtspUnitId,
                                    streamer.getSessionId(),
                                    listenIp,
                                    listenRtspPort
                            );

                            NettyChannelManager.getInstance().deleteStreamer(rtspUnitId, streamer.getSessionId(), listenIp, listenRtspPort);
                            rtspUnit.setStreamer(null);
                            logger.debug("({}) ({}) ({}) Finish to stream the media. All media segment is played.", name, rtspUnit.getRtspUnitId(), streamer.getSessionId());
                        }
                        ///////////////////////////////////////////////////////////////////////////

                        rtspStateHandler.fire(
                                RtspEvent.TEARDOWN_OK,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );

                        res.setStatus(RtspResponseStatuses.OK);
                        res.headers().add(
                                RtspHeaderNames.SESSION,
                                curSessionId
                        );

                        sendResponse(name, rtspUnit, streamer, ctx, req, res);
                    }
                }
                ///////////////////////////////////////////////////////////////////////////

                ///////////////////////////////////////////////////////////////////////////
                // 6) PAUSE
                else if (req.method() == RtspMethods.PAUSE) {
                    logger.debug("({}) ({}) () < PAUSE", name, rtspUnit.getRtspUnitId());

                    if (curState.equals(RtspState.PLAY)) {
                        rtspStateHandler.fire(
                                RtspEvent.PAUSE,
                                rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                        );

                        ///////////////////////////////////////////////////////////////////////////
                        String curSessionId = req.headers().get(RtspHeaderNames.SESSION);
                        if (curSessionId == null) {
                            logger.warn("({}) ({}) () SessionId is null. Fail to process PAUSE method. (listenIp={}, listenPort={})", name, rtspUnit.getRtspUnitId(), listenIp, listenRtspPort);
                            rtspStateHandler.fire(
                                    RtspEvent.PAUSE_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                            sendFailResponse(name, rtspUnit, null, ctx, req, res, null, RtspResponseStatuses.NOT_ACCEPTABLE);
                            return;
                        }
                        logger.debug("({}) ({}) () Current sessionId is [{}]. (listenIp={}, listenPort={})", name, rtspUnit.getRtspUnitId(), curSessionId, listenIp, listenRtspPort);

                        Streamer streamer = NettyChannelManager.getInstance().getStreamer(rtspUnitId, curSessionId, listenIp, listenRtspPort);
                        if (streamer == null) {
                            logger.warn("({}) ({}) () Streamer is not defined. (listenIp={}, listenPort={})", name, rtspUnit.getRtspUnitId(), listenIp, listenRtspPort);
                            rtspStateHandler.fire(
                                    RtspEvent.PAUSE_FAIL,
                                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                            );
                            sendFailResponse(name, rtspUnit, null, ctx, req, res, curSessionId, RtspResponseStatuses.INTERNAL_SERVER_ERROR);
                            return;
                        }

                        if (NettyChannelManager.getInstance().getStreamer(rtspUnitId, streamer.getSessionId(), listenIp, listenRtspPort) != null) {
                            logger.debug("({}) ({}) ({}) Pause the streaming. (rtpDestPort={})", name, rtspUnit.getRtspUnitId(), streamer.getSessionId(), streamer.getDestPort());
                            NettyChannelManager.getInstance().pauseStreaming(
                                    rtspUnitId,
                                    streamer.getSessionId(),
                                    listenIp,
                                    listenRtspPort
                            );
                        }

                        streamer.resetSeqAndTime();
                        ///////////////////////////////////////////////////////////////////////////

                        res.setStatus(RtspResponseStatuses.OK);
                        res.headers().add(
                                RtspHeaderNames.SESSION,
                                curSessionId
                        );
                        sendResponse(name, rtspUnit, streamer, ctx, req, res);
                    }
                }
                ///////////////////////////////////////////////////////////////////////////

                ///////////////////////////////////////////////////////////////////////////
                // 7) UNKNOWN
                else {
                    logger.warn("({}) ({}) () < Unknown method: {}", name, rtspUnit.getRtspUnitId(), req.method());
                    sendFailResponse(name, rtspUnit, null, ctx, req, res, null, RtspResponseStatuses.METHOD_NOT_ALLOWED);
                    ctx.write(res).addListener(ChannelFutureListener.CLOSE);
                }
                ///////////////////////////////////////////////////////////////////////////
            }
        } catch (Exception e) {
            logger.warn("({}) ({}) Fail to handle UDP Packet.", name, rtspUnitId, e);
        }
    }

    public static void sendResponse(String name, RtspUnit rtspUnit, Streamer streamer, ChannelHandlerContext ctx, DefaultHttpRequest req, FullHttpResponse res) {
        final String cSeq = req.headers().get(RtspHeaderNames.CSEQ);
        if (cSeq != null) {
            res.headers().add(RtspHeaderNames.CSEQ, cSeq);
        }

        //res.headers().set(RtspHeaderNames.CONNECTION, RtspHeaderValues.KEEP_ALIVE);
        ctx.write(res);

        String sessionId = null;
        if (streamer != null) {
            sessionId = streamer.getSessionId();
        }

        String rtspUnitId = null;
        if (rtspUnit != null) {
            rtspUnitId = rtspUnit.getRtspUnitId();
        }

        logger.debug("({}) ({}) ({}) Response: {}", name, rtspUnitId, sessionId, res);
        logger.debug("({}) ({}) ({}) > Send response. ({})", name, rtspUnitId, sessionId, req.method());
    }

    public void sendFailResponse(String name, RtspUnit rtspUnit, Streamer streamer, ChannelHandlerContext ctx, DefaultHttpRequest req, FullHttpResponse res, String curSessionId, HttpResponseStatus httpResponseStatus) {
        res.setStatus(httpResponseStatus);
        if (curSessionId != null && curSessionId.length() > 0) {
            res.headers().add(
                    RtspHeaderNames.SESSION,
                    curSessionId
            );
        }
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
