package com.rtsp.module.netty.handler;

import com.rtsp.module.RtspManager;
import com.rtsp.module.base.RtspUnit;
import com.rtsp.protocol.RtcpPacket;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

/**
 * @class public class RtcpChannelHandler extends SimpleChannelInboundHandler<DatagramPacket>
 */
public class RtcpChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(RtcpChannelHandler.class);

    private final String name;

    ////////////////////////////////////////////////////////////////////////////////

    public RtcpChannelHandler(String name) {
        this.name = name;
        logger.debug("RtcpChannelHandler is created. (name={})", name);
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @param ctx ChannelHandlerContext {@link ChannelHandlerContext}
     * @param msg UDP 패킷 데이터
     * @fn protected void channelRead0 (ChannelHandlerContext ctx, DatagramPacket msg)
     * @brief UDP 패킷을 Media Server 으로부터 수신하는 함수
     */
    @Override
    protected void channelRead0 (ChannelHandlerContext ctx, DatagramPacket msg) {
        try {
            RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
            if (rtspUnit == null) {
                return;
            }

            ByteBuf buf = msg.content();
            if (buf == null) {
                return;
            }

            int readBytes = buf.readableBytes();
            if (readBytes <= 0) {
                return;
            }

            byte[] data = new byte[readBytes];
            buf.getBytes(0, data);

            RtcpPacket rtcpPkt = new RtcpPacket(data, data.length);
            logger.debug("[RTCP] {}", rtcpPkt);

            //set congestion level between 0 to 4
            float fractionLost = rtcpPkt.fractionLost;
            if (fractionLost >= 0 && fractionLost <= 0.01) {
                rtspUnit.setCongestionLevel(0); //less than 0.01 assume negligible
            }
            else if (fractionLost > 0.01 && fractionLost <= 0.25) {
                rtspUnit.setCongestionLevel(1);
            }
            else if (fractionLost > 0.25 && fractionLost <= 0.5) {
                rtspUnit.setCongestionLevel(2);
            }
            else if (fractionLost > 0.5 && fractionLost <= 0.75) {
                rtspUnit.setCongestionLevel(3);
            }
            else {
                rtspUnit.setCongestionLevel(4);
            }
        } catch (Exception e) {
            logger.warn("| ({}) Fail to handle UDP Packet.", name, e);
        }
    }

    @Override
    public void exceptionCaught (ChannelHandlerContext ctx, Throwable cause) {
        //logger.warn("| ServerHandler.exceptionCaught", cause);
        //ctx.close();
    }

}
