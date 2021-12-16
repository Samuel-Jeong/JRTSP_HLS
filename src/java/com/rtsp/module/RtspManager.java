package com.rtsp.module;

import com.rtsp.fsm.RtspState;
import com.rtsp.module.base.RtspUnit;
import com.rtsp.module.netty.NettyChannelManager;
import com.rtsp.service.ResourceManager;

/**
 * @class public class RtspManager
 * @brief RtspManager class
 */
public class RtspManager {

    public static final String RTSP_RES_SESSION = "Session";

    private static RtspManager rtspManager = null;

    private RtspUnit rtspUnit = null;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspManager() {
        // Nothing
    }

    public static RtspManager getInstance ( ) {
        if (rtspManager == null) {
            rtspManager = new RtspManager();
        }

        return rtspManager;
    }

    public void openRtspUnit(String ip, int port) {
        if (rtspUnit == null) {
            rtspUnit = new RtspUnit(ip, port);
            rtspUnit.getStateManager().addStateUnit(
                    rtspUnit.getRtspStateUnitId(),
                    rtspUnit.getStateManager().getStateHandler(RtspState.NAME).getName(),
                    RtspState.INIT,
                    null
            );
        }
    }

    public void closeRtspUnit() {
        if (rtspUnit != null) {
            NettyChannelManager.getInstance().deleteChannel(
                    rtspUnit.getRtspChannel().getListenIp()
                            + "_" +
                            rtspUnit.getRtspChannel().getListenPort()
            );

            NettyChannelManager.getInstance().deleteChannel(
                    rtspUnit.getRtcpChannel().getListenIp()
                            + "_" +
                            rtspUnit.getRtcpChannel().getListenPort()
            );

            int port = rtspUnit.getClientPort();
            if (port > 0) {
                ResourceManager.getInstance().restorePort(port);
            }
        }
    }

    public RtspUnit getRtspUnit() {
        return rtspUnit;
    }

}
