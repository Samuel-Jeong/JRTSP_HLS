package com.rtsp.module.netty.handler;

import com.rtsp.module.MessageSender;
import com.rtsp.service.TaskManager;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
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
    private String session = null;

    private final Random random = new Random();

    MessageSender messageSender = null;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspChannelHandler(String name) {
        this.name = name;
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
                FullHttpResponse rep = new DefaultFullHttpResponse(RtspVersions.RTSP_1_0,  RtspResponseStatuses.NOT_FOUND);

                logger.debug("Request: {}", req);

                if (req.method() == RtspMethods.OPTIONS) {
                    rep.setStatus(RtspResponseStatuses.OK);
                    rep.headers().add(RtspHeaderValues.PUBLIC, "DESCRIBE, SETUP, PLAY, TEARDOWN");
                    sendAnswer(ctx, req, rep);
                } else if (req.method() == RtspMethods.DESCRIBE) {
                    ByteBuf buf = Unpooled.copiedBuffer("c=IN IP4 127.0.0.1\r\nm=video 5004 RTP/AVP 96\r\na=rtpmap:96 H264/90000\r\n", CharsetUtil.UTF_8);
                    rep.setStatus(RtspResponseStatuses.OK);
                    rep.headers().add(RtspHeaderNames.CONTENT_TYPE, "application/sdp");
                    rep.headers().add(RtspHeaderNames.CONTENT_LENGTH, buf.writerIndex());
                    rep.content().writeBytes(buf);
                    sendAnswer(ctx, req, rep);
                } else if (req.method() == RtspMethods.SETUP) {
                    rep.setStatus(RtspResponseStatuses.OK);
                    session = String.format("%08x", random.nextInt(65536));
                    rep.headers().add(RtspHeaderNames.SESSION, session);
                    rep.headers().add(RtspHeaderNames.TRANSPORT,"RTP/AVP;unicast;client_port=5004-5005");
                    sendAnswer(ctx, req, rep);
                } else if (req.method() == RtspMethods.PLAY) {
                    rep.setStatus(RtspResponseStatuses.OK);
                    sendAnswer(ctx, req, rep);

                    // TODO: send image of video
                    if (messageSender == null) {
                        messageSender = new MessageSender(session, "127.0.0.1", 5004 );
                    }


                    //messageSender.send();
                } else {
                    logger.warn("Unknown method: {}", req.method());
                    ctx.write(rep).addListener(ChannelFutureListener.CLOSE);
                }
            }
        } catch (Exception e) {
            logger.warn("| ({}) Fail to handle UDP Packet.", name, e);
        }
    }

    private void sendAnswer(ChannelHandlerContext ctx, DefaultHttpRequest req, FullHttpResponse res) {
        final String cSeq = req.headers().get(RtspHeaderNames.CSEQ);
        if (cSeq != null) {
            res.headers().add(RtspHeaderNames.CSEQ, cSeq);
        }

        final String session = req.headers().get(RtspHeaderNames.SESSION);
        if (session != null) {
            res.headers().add(RtspHeaderNames.SESSION, session);
        }

        logger.debug("Response: {}", res);

        res.headers().set(RtspHeaderNames.CONNECTION, RtspHeaderValues.KEEP_ALIVE);
        ctx.write(res);
    }

}
