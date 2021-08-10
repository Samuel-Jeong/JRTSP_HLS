package com.rtsp.module.netty.handler;

import com.rtsp.module.RtspManager;
import com.rtsp.module.base.RtspUnit;
import com.rtsp.module.netty.NettyChannelManager;
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

import java.util.Random;

/**
 * @class public class RtspChannelHandler extends ChannelInboundHandlerAdapter
 * @brief RtspChannelHandler class
 */
public class RtspChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RtspChannelHandler.class);

    private final String name;

    private final String listenIp; // local ip
    private final int listenPort; // local(listen) port

    private final Random random = new Random();

    ////////////////////////////////////////////////////////////////////////////////

    public RtspChannelHandler(String listenIp, int listenPort) {
        this.name = listenIp + ":" + listenPort;

        this.listenIp = listenIp;
        this.listenPort = listenPort;


        RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
        if (rtspUnit == null) {
            logger.warn("Fail to get the rtsp unit.");
            logger.debug("Fail to create RtspChannelHandler. (listenIp={}, listenPort={})", listenIp, listenPort);
            return;
        }
        rtspUnit.setSsrc();

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

                    // TODO: Set port to client
                    ByteBuf buf = Unpooled.copiedBuffer(
                            "v=0\r\n" +
                                    "c=IN IP4 " + listenIp + "\r\n" +
                                    "t=0\r\n" +
                                    "m=video 5004 RTP/AVP 96\r\n" +
                                    "a=rtpmap:96 H264/90000",
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
                        rtspUnit.setSessionId(String.valueOf(random.nextInt(Integer.MAX_VALUE)));
                    } else {
                        logger.warn("Unknown transport header content. ({})", clientPortString);
                        return;
                    }

                    String sessionId = rtspUnit.getSessionId();
                    int destPort = rtspUnit.getDestPort();
                    if (destPort > 0 && sessionId != null) {
                        NettyChannelManager.getInstance().addMessageSender(
                                sessionId,
                                listenIp,
                                listenPort,
                                rtspUnit.getDestIp(),
                                rtspUnit.getDestPort(),
                                rtspUnit.getRtcpDestPort(),
                                req.uri()
                        );

                        res.headers().add(
                                RtspHeaderNames.SESSION,
                                sessionId
                        );

                        res.headers().add(
                                RtspHeaderNames.TRANSPORT,
                                "RTP/AVP;unicast;client_port=" + destPort
                        );
                        /*res.headers().add(
                                RtspHeaderNames.TRANSPORT,
                                "ssrc=" + rtspUnit.getSsrc()
                        );*/

                        res.setStatus(RtspResponseStatuses.OK);
                        sendResponse(ctx, req, res);

                        logger.debug("({}) Setup to stream the media. (rtpDestPort={})", rtspUnit.getSessionId(), destPort);
                    } else {
                        logger.warn("({}) Fail to send the response for SETUP. (rtpDestPort={})", sessionId, destPort);
                    }
                }
                // 4) PLAY
                else if (req.method() == RtspMethods.PLAY) {
                    logger.debug("< PLAY");

                    res.setStatus(RtspResponseStatuses.OK);
                    sendResponse(ctx, req, res);

                    int destPort = rtspUnit.getDestPort();
                    if (destPort > 0) {
                        logger.debug("({}) Start to stream the media. (rtpDestPort={})", rtspUnit.getSessionId(), destPort);
                        NettyChannelManager.getInstance().startStreaming(
                                rtspUnit.getSessionId(),
                                listenIp,
                                listenPort
                        );
                    }
                }
                // 5) TEARDOWN
                else if (req.method() == RtspMethods.TEARDOWN) {
                    logger.debug("< TEARDOWN");

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
