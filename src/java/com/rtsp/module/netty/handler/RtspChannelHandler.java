package com.rtsp.module.netty.handler;

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
import java.util.UUID;

/**
 * @class public class RtspChannelHandler extends ChannelInboundHandlerAdapter
 * @brief RtspChannelHandler class
 */
public class RtspChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RtspChannelHandler.class);

    private final String name;

    private final String listenIp; // local ip
    private final int listenPort; // local(listen) port

    private final String destIp;
    private int destPort = -1; // rtp destination port

    private String session = null;

    private final Random random = new Random();

    ////////////////////////////////////////////////////////////////////////////////

    public RtspChannelHandler(String listenIp, int listenPort) {
        this.name = listenIp + ":" + listenPort;

        this.destIp = listenIp;

        this.listenIp = listenIp;
        this.listenPort = listenPort;
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

                DefaultHttpRequest req = (DefaultHttpRequest) msg;
                FullHttpResponse res = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0,  RtspResponseStatuses.NOT_FOUND);

                //logger.debug("Request: {}", req);

                // 1) OPTIONS
                if (req.method() == RtspMethods.OPTIONS) {
                    res.setStatus(RtspResponseStatuses.OK);
                    res.headers().add(
                            RtspHeaderValues.PUBLIC,
                            "DESCRIBE, PAUSE, SETUP, PLAY, TEARDOWN"
                    );
                    sendResponse(ctx, req, res);
                }
                // 2) DESCRIBE
                else if (req.method() == RtspMethods.DESCRIBE) {
                    // TODO: Set port to client
                    ByteBuf buf = Unpooled.copiedBuffer(
                            "c=IN IP4 " + listenIp + "\r\n" +
                                    "m=video 5004 RTP/AVP 96\r\n" +
                                    "a=rtpmap:96 H264/90000\r\n",
                            CharsetUtil.UTF_8
                    );

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
                    String transportHeaderContent = req.headers().get(RtspHeaderNames.TRANSPORT);
                    String clientPortString = transportHeaderContent.substring(
                            transportHeaderContent.lastIndexOf(";") + 1
                    );
                    String rtpDesPortString = clientPortString.substring(
                            clientPortString.lastIndexOf("=") + 1
                    );
                    if (rtpDesPortString.contains("-")) {
                        rtpDesPortString = rtpDesPortString.substring(
                                0,
                                rtpDesPortString.lastIndexOf("-")
                        );
                    }

                    int rtpDestPort = Integer.parseInt(rtpDesPortString);
                    this.destPort = rtpDestPort;
                    if (destPort <= 0) {
                        return;
                    }

                    res.setStatus(RtspResponseStatuses.OK);
                    session = String.valueOf(UUID.randomUUID());

                    NettyChannelManager.getInstance().addMessageSender(
                            session,
                            listenIp,
                            listenPort,
                            destIp,
                            rtpDestPort,
                            req.uri()
                    );

                    res.headers().add(
                            RtspHeaderNames.SESSION,
                            session
                    );
                    res.headers().add(
                            RtspHeaderNames.TRANSPORT,
                            "RTP/AVP;unicast;client_port=" + rtpDestPort
                    );

                    sendResponse(ctx, req, res);
                }
                // 4) PLAY
                else if (req.method() == RtspMethods.PLAY) {
                    res.setStatus(RtspResponseStatuses.OK);
                    sendResponse(ctx, req, res);

                    if (destPort > 0) {
                        logger.debug("Start to stream the media. (rtpDestPort={})", destPort);
                        NettyChannelManager.getInstance().startStreaming(session, listenIp, listenPort);
                    }
                }
                // 5) TEARDOWN
                else if (req.method() == RtspMethods.TEARDOWN) {
                    res.setStatus(RtspResponseStatuses.OK);
                    sendResponse(ctx, req, res);

                    if (destPort > 0) {
                        logger.debug("Stop to stream the media. (rtpDestPort={})", destPort);
                        NettyChannelManager.getInstance().stopStreaming(session, listenIp, listenPort);
                        NettyChannelManager.getInstance().deleteMessageSender(
                                session,
                                listenIp,
                                listenPort
                        );

                        destPort = -1;
                    }
                }
                // 6) UNKNOWN
                else {
                    logger.warn("Unknown method: {}", req.method());
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

        //logger.debug("Response: {}", res);

        res.headers().set(RtspHeaderNames.CONNECTION, RtspHeaderValues.KEEP_ALIVE);
        ctx.write(res);
    }

}
