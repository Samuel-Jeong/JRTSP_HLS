package rtsp.module.netty.handler;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.socket.DatagramPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.module.RtspManager;
import rtsp.module.base.RtspUnit;
import rtsp.protocol.base.ByteUtil;
import rtsp.protocol.rtcp.base.RtcpType;
import rtsp.protocol.rtcp.packet.RtcpPacket;
import rtsp.protocol.rtcp.type.regular.RtcpReceiverReport;
import rtsp.protocol.rtcp.type.regular.base.RtcpHeader;
import rtsp.protocol.rtcp.type.regular.base.report.RtcpReportBlock;

/**
 * @class public class RtcpChannelHandler extends SimpleChannelInboundHandler<DatagramPacket>
 */
public class RtcpChannelHandler extends SimpleChannelInboundHandler<DatagramPacket> {

    private static final Logger logger = LoggerFactory.getLogger(RtcpChannelHandler.class);

    private final String rtspUnitId;
    private final String name;
    private final String listenIp;
    private final int listenPort;

    ////////////////////////////////////////////////////////////////////////////////

    public RtcpChannelHandler(String rtspUnitId, String listenIp, int listenPort) {
        this.name = "RTCP_" + rtspUnitId + "_" + listenIp + ":" + listenPort;

        this.rtspUnitId = rtspUnitId;
        this.listenIp = listenIp;
        this.listenPort = listenPort;

        logger.debug("({}) RtcpChannelHandler is created. (listenIp={}, listenPort={})", name, listenIp, listenPort);
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void channelRead0 (ChannelHandlerContext ctx, DatagramPacket msg) {
        try {
            RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit(rtspUnitId);
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

            logger.debug("({}) data: [{}], readBytes: [{}]", name, ByteUtil.byteArrayToHex(data), readBytes);

            if (data.length >= RtcpHeader.LENGTH) {
                RtcpPacket rtcpPacket = new RtcpPacket(data);
                logger.debug("({}) {}", name, rtcpPacket);

                int packetType = rtcpPacket.getRtcpHeader().getPacketType();
                switch (packetType) {
                    case RtcpType.RECEIVER_REPORT:
                        RtcpReceiverReport rtcpReceiverReport = (RtcpReceiverReport) rtcpPacket.getRtcpFormat();
                        RtcpReportBlock rtcpReportBlock = rtcpReceiverReport.getReportBlockByIndex(0);
                        if (rtcpReportBlock != null) {
                            float fractionLost = rtcpReportBlock.getFraction();
                            if (fractionLost >= 0 && fractionLost <= 0.01) {
                                rtspUnit.setCongestionLevel(0);
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
                        }
                        break;
                    default:
                        break;
                }
            }
        } catch (Exception e) {
            logger.warn("| ({}) Fail to handle the rtcp Packet.", name, e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getName() {
        return name;
    }

    public String getListenIp() {
        return listenIp;
    }

    public int getListenPort() {
        return listenPort;
    }
}
