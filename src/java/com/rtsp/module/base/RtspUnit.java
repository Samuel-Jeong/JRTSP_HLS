package com.rtsp.module.base;

import com.fsm.StateManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rtsp.fsm.RtspFsmManager;
import com.rtsp.module.Streamer;
import com.rtsp.module.netty.NettyChannelManager;
import com.rtsp.module.netty.base.NettyChannelType;
import com.rtsp.module.netty.module.NettyChannel;
import com.rtsp.service.AppInstance;
import com.rtsp.service.ResourceManager;

import java.util.UUID;

/**
 * @class public class RtspUnit
 * @brief RtspUnit class
 */
public class RtspUnit {

    private static final Logger logger = LoggerFactory.getLogger(RtspUnit.class);

    private final String rtspId = UUID.randomUUID().toString(); // ID of the RTSP session

    private int congestionLevel = 0;

    private final NettyChannel rtspChannel;
    private final NettyChannel rtcpChannel;
    private int clientPort = 0;

    // TODO: Must manage the streamers
    private Streamer streamer = null;
    //

    private final RtspFsmManager rtspFsmManager = new RtspFsmManager();
    private final String rtspStateUnitId;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspUnit(String listenIp, int listenPort) {
        this.rtspStateUnitId = String.valueOf(UUID.randomUUID());

        int rtcpListenPort = AppInstance.getInstance().getConfigManager().getLocalRtcpListenPort();
        rtspChannel = NettyChannelManager.getInstance().openChannel(listenIp, listenPort, NettyChannelType.RTSP);
        rtcpChannel = NettyChannelManager.getInstance().openChannel(listenIp, rtcpListenPort, NettyChannelType.RTCP);

        rtspFsmManager.init(this);
    }

    ////////////////////////////////////////////////////////////////////////////////

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

    public String getRtspId() {
        return rtspId;
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

    public NettyChannel getRtcpChannel() {
        return rtcpChannel;
    }

    public int getClientPort() {
        return clientPort;
    }

    public void setClientPort(int clientPort) {
        if (clientPort <= 0) {
            logger.warn("({}) RtspUnit clientPort is not set up. ({})", rtspId, clientPort);
            return;
        }

        if (this.clientPort != clientPort) {
            this.clientPort = clientPort;
            ResourceManager.getInstance().restorePort(clientPort);
            logger.debug("({}) RtspUnit clientPort is set up. ({})", rtspId, clientPort);
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
