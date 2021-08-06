package com.rtsp.module;

import com.rtsp.module.base.RtspUnit;

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
        }
    }

    public void closeRtspUnit() {
        if (rtspUnit != null) {
            rtspUnit.getRtspChannel().closeChannel();
            rtspUnit.getRtcpChannel().closeChannel();
        }
    }

    public RtspUnit getRtspUnit() {
        return rtspUnit;
    }

}
