package com.rtsp.module.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;

/**
 * @class public class RtspChannelHandler extends ChannelInboundHandlerAdapter
 * @brief RtspChannelHandler class
 */
public class MessageSenderChannelHandler extends ChannelInboundHandlerAdapter {

    ////////////////////////////////////////////////////////////////////////////////

    public MessageSenderChannelHandler() {
        // Nothing
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void channelRead (ChannelHandlerContext ctx, Object msg) {
        // Nothing
    }

}
