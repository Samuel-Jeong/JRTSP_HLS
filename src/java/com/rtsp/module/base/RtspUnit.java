package com.rtsp.module.base;

import com.rtsp.module.netty.NettyChannelManager;
import com.rtsp.module.netty.base.NettyChannelType;
import com.rtsp.module.netty.module.NettyChannel;
import java.util.UUID;

/**
 * @class public class RtspUnit
 * @brief RtspUnit class
 */
public class RtspUnit {

    private String rtspId = UUID.randomUUID().toString(); // ID of the RTSP session
    private int state = RtspState.INIT;

    private String VideoFileName;
    private int rtspDestPort = 0;

    private static final int RTCP_RCV_PORT = 19001; // port where the client will receive the RTP packets
    private static final int RTCP_PERIOD = 400;     // How often to check for control events
    private int congestionLevel = 0;

    private NettyChannel rtspChannel;
    private NettyChannel rtcpChannel;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspUnit(String ip, int port) {
        rtspChannel = NettyChannelManager.getInstance().openChannel(ip, port, NettyChannelType.RTSP);
        rtcpChannel = NettyChannelManager.getInstance().openChannel(ip, RTCP_RCV_PORT, NettyChannelType.RTCP);
    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getRtspId() {
        return rtspId;
    }

    public void setRtspId(String rtspId) {
        this.rtspId = rtspId;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public String getVideoFileName() {
        return VideoFileName;
    }

    public void setVideoFileName(String videoFileName) {
        VideoFileName = videoFileName;
    }

    public int getRtspDestPort() {
        return rtspDestPort;
    }

    public void setRtspDestPort(int rtspDestPort) {
        this.rtspDestPort = rtspDestPort;
    }

    public static int getRtcpRcvPort() {
        return RTCP_RCV_PORT;
    }

    public static int getRtcpPeriod() {
        return RTCP_PERIOD;
    }

    public int getCongestionLevel() {
        return congestionLevel;
    }

    public void setCongestionLevel(int congestionLevel) {
        this.congestionLevel = congestionLevel;
    }

    public NettyChannel getRtspChannel() {
        return rtspChannel;
    }

    public void setRtspChannel(NettyChannel rtspChannel) {
        this.rtspChannel = rtspChannel;
    }

    public NettyChannel getRtcpChannel() {
        return rtcpChannel;
    }

    public void setRtcpChannel(NettyChannel rtcpChannel) {
        this.rtcpChannel = rtcpChannel;
    }
}
