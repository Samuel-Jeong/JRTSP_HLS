package com.rtsp.module.netty.handler;

import com.rtsp.module.netty.NettyChannelManager;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @class public class MessageSenderChannelHandler extends ChannelInboundHandlerAdapter
 * @brief MessageSenderChannelHandler class
 */
public class MessageSenderChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MessageSenderChannelHandler.class);

    private final String id;
    private final String listenIp;
    private final int listenPort;

    ////////////////////////////////////////////////////////////////////////////////

    public MessageSenderChannelHandler(String id, String listenIp, int listenPort) {
        this.id = id;
        this.listenIp = listenIp;
        this.listenPort = listenPort;
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // Nothing
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.warn("MessageSender is deleted by channel inactivity.");
        NettyChannelManager.getInstance().deleteMessageSender(
                id,
                listenIp,
                listenPort
        );
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.warn("MessageSender is deleted by channel exception. (cause={})", cause.toString());
        NettyChannelManager.getInstance().deleteMessageSender(
                id,
                listenIp,
                listenPort
        );
    }

}