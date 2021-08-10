package com.rtsp.module.base;

import com.rtsp.module.netty.NettyChannelManager;
import com.rtsp.module.netty.base.NettyChannelType;
import com.rtsp.module.netty.module.NettyChannel;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @class public class RtspUnit
 * @brief RtspUnit class
 */
public class RtspUnit {

    private String rtspId = UUID.randomUUID().toString(); // ID of the RTSP session
    private int state = RtspState.INIT;

    private String fileName;

    private int congestionLevel = 0;

    private NettyChannel rtspChannel;
    private NettyChannel rtcpChannel;

    private final AtomicBoolean isInterleaved = new AtomicBoolean(false);

    private String sessionId = null;
    private int ssrc;

    private String destIp;
    private int destPort = -1; // rtp destination port

    private static final int rtcpListenPort = 19001; // rtcp listen port
    private int rtcpDestPort = -1; // rtcp destination port

    private final Random random = new Random();

    ////////////////////////////////////////////////////////////////////////////////

    public RtspUnit(String listenIp, int listenPort) {
        rtspChannel = NettyChannelManager.getInstance().openChannel(listenIp, listenPort, NettyChannelType.RTSP);
        rtcpChannel = NettyChannelManager.getInstance().openChannel(listenIp, rtcpListenPort, NettyChannelType.RTCP);

        ssrc = random.nextInt(Integer.MAX_VALUE);
    }

    ////////////////////////////////////////////////////////////////////////////////

    public int getSsrc() {
        return ssrc;
    }

    public void setSsrc() {
        this.ssrc = random.nextInt(Integer.MAX_VALUE);
    }

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

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getRtcpDestPort() {
        return rtcpDestPort;
    }

    public void setRtcpDestPort(int rtcpDestPort) {
        this.rtcpDestPort = rtcpDestPort;
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

    public boolean isInterleaved() {
        return isInterleaved.get();
    }

    public void setInterleaved(boolean interleaved) {
        isInterleaved.set(interleaved);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDestIp() {
        return destIp;
    }

    public void setDestIp(String destIp) {
        this.destIp = destIp;
    }

    public int getDestPort() {
        return destPort;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }
}
