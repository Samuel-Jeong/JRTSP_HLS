package com.rtsp.module.base;

import com.fsm.StateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rtsp.fsm.RtspFsmManager;
import com.rtsp.module.Streamer;
import com.rtsp.module.netty.NettyChannelManager;
import com.rtsp.module.netty.module.RtcpNettyChannel;
import com.rtsp.module.netty.module.RtspNettyChannel;
import com.rtsp.module.sdp.base.Sdp;
import com.rtsp.service.AppInstance;
import com.rtsp.service.ResourceManager;

import java.util.UUID;

/**
 * @class public class RtspUnit
 * @brief RtspUnit class
 */
public class RtspUnit {

    private static final Logger logger = LoggerFactory.getLogger(RtspUnit.class);

    private final String rtspUnitId; // ID of the RTSP session by client
    private long sessionId = 0; // ID of the session

    private int congestionLevel = 0;

    private final RtspNettyChannel rtspChannel;
    private final RtcpNettyChannel rtcpChannel;
    private int serverPort = 0;

    // TODO: Must manage the streamers
    private Streamer streamer = null;
    //

    private final RtspFsmManager rtspFsmManager = new RtspFsmManager();
    private final String rtspStateUnitId;

    private Sdp sdp = null;

    private boolean isRegistered = false;

    private double fileTime = 0.0;
    private double startTime = 0.0;
    private double endTime = 0.0;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspUnit(String rtspUnitId, String listenIp, int listenPort) {
        this.rtspUnitId = rtspUnitId;
        this.rtspStateUnitId = String.valueOf(UUID.randomUUID());

        int rtcpListenPort = AppInstance.getInstance().getConfigManager().getLocalRtcpListenPort();
        rtspChannel = NettyChannelManager.getInstance().openRtspChannel(rtspUnitId, listenIp, listenPort);
        rtcpChannel = NettyChannelManager.getInstance().openRtcpChannel(rtspUnitId, listenIp, rtcpListenPort);

        rtspFsmManager.init(this);
    }

    ////////////////////////////////////////////////////////////////////////////////

    public double getFileTime() {
        return fileTime;
    }

    public void setFileTime(double fileTime) {
        this.fileTime = fileTime;
    }

    public double getStartTime() {
        return startTime;
    }

    public void setStartTime(double startTime) {
        this.startTime = startTime;
    }

    public double getEndTime() {
        return endTime;
    }

    public void setEndTime(double endTime) {
        this.endTime = endTime;
    }

    public boolean isRegistered() {
        return isRegistered;
    }

    public void setRegistered(boolean registered) {
        isRegistered = registered;
    }

    public long getSessionId() {
        return sessionId;
    }

    public void setSessionId(long sessionId) {
        this.sessionId = sessionId;
    }

    public Sdp getSdp() {
        return sdp;
    }

    public void setSdp(Sdp sdp) {
        this.sdp = sdp;
    }

    public StateManager getStateManager() {
        return rtspFsmManager.getStateManager();
    }

    public String getRtspStateUnitId() {
        return rtspStateUnitId;
    }

    public Streamer getStreamer() {
        return streamer;
    }

    public void setStreamer(Streamer streamer) {
        this.streamer = streamer;
    }

    public String getRtspUnitId() {
        return rtspUnitId;
    }

    public int getCongestionLevel() {
        return congestionLevel;
    }

    public void setCongestionLevel(int congestionLevel) {
        this.congestionLevel = congestionLevel;
    }

    public RtspNettyChannel getRtspChannel() {
        return rtspChannel;
    }

    public RtcpNettyChannel getRtcpChannel() {
        return rtcpChannel;
    }

    public int getServerPort() {
        return serverPort;
    }

    public void setServerPort(int serverPort) {
        if (serverPort <= 0) {
            logger.warn("({}) RtspUnit serverPort is not set up. ({})", rtspUnitId, serverPort);
            return;
        }

        if (this.serverPort != serverPort) {
            this.serverPort = serverPort;
            ResourceManager.getInstance().restorePort(serverPort);
            logger.debug("({}) RtspUnit serverPort is set up. ({})", rtspUnitId, serverPort);
        }
    }

    /////////////////////////////////////////////////////////////////////

    public static String getPureFileName(String uri) {
        if (uri.startsWith("rtsp")) {
            String curFileName = uri;
            String rtspStr = curFileName.substring(
                    curFileName.lastIndexOf("rtsp:") + 8
            );

            curFileName = rtspStr.substring(
                    rtspStr.indexOf("/")
            );

            if (curFileName.charAt(curFileName.length() - 1) == '/') {
                curFileName = curFileName.substring(
                        0,
                        curFileName.length() - 1
                );
            }

            return curFileName;
        }

        return uri;
    }

}
