package com.rtsp.module;

import com.rtsp.fsm.RtspFsmManager;
import com.rtsp.module.base.RtspUnit;
import com.rtsp.module.netty.NettyChannelManager;

/**
 * @class public class RtspManager
 * @brief RtspManager class
 */
public class RtspManager {

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
            RtspFsmManager.getInstance().init(rtspUnit);
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
        }
    }

    public RtspUnit getRtspUnit() {
        return rtspUnit;
    }

}
