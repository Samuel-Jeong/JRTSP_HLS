package com.rtsp.module.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @class public class StreamerChannelHandler extends ChannelInboundHandlerAdapter
 * @brief StreamerChannelHandler class
 */
public class StreamerChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(StreamerChannelHandler.class);

    private final String id;

    ////////////////////////////////////////////////////////////////////////////////

    public StreamerChannelHandler(String id) {
        this.id = id;
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // Nothing
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.warn("({}) StreamerChannelHandler is inactive.", id);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("({}) StreamerChannelHandler.Exception. (cause={})", id, cause.toString());
    }

}