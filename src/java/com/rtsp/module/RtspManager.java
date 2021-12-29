package com.rtsp.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rtsp.fsm.RtspState;
import com.rtsp.module.base.RtspUnit;
import com.rtsp.module.netty.NettyChannelManager;
import com.rtsp.service.ResourceManager;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @class public class RtspManager
 * @brief RtspManager class
 */
public class RtspManager {

    private static final Logger logger = LoggerFactory.getLogger(RtspManager.class);

    public static final String RTSP_RES_SESSION = "Session";

    private static RtspManager rtspManager = null;

    private final HashMap<String, RtspUnit> rtspUnitMap = new HashMap<>();
    private final ReentrantLock rtspUnitMapLock = new ReentrantLock();

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

    public void openRtspUnit(String rtspUnitId, String ip, int port) {
        try {
            rtspUnitMapLock.lock();

            RtspUnit rtspUnit = new RtspUnit(rtspUnitId, ip, port);
            rtspUnit.getStateManager().addStateUnit(
                    rtspUnit.getRtspStateUnitId(),
                    rtspUnit.getStateManager().getStateHandler(RtspState.NAME).getName(),
                    RtspState.IDLE,
                    null
            );

            rtspUnitMap.putIfAbsent(rtspUnitId, rtspUnit);
        } catch (Exception e) {
            logger.warn("Fail to open the rtsp unit. (id={}, ip={}, port={})", rtspUnitId, ip, port, e);
        } finally {
            rtspUnitMapLock.unlock();
        }
    }

    // Unregister 요청받거나 Register 요청 거부할 때 사용
    public void closeRtspUnit(String rtspUnitId) {
        try {
            rtspUnitMapLock.lock();

            RtspUnit rtspUnit = getRtspUnit(rtspUnitId);
            NettyChannelManager.getInstance().deleteRtspChannel(rtspUnitId);
            NettyChannelManager.getInstance().deleteRtcpChannel(rtspUnitId);

            int port = rtspUnit.getClientRtpListenPort();
            if (port > 0) {
                ResourceManager.getInstance().restorePort(port);
            }

            rtspUnitMap.remove(rtspUnitId);
        } catch (Exception e) {
            logger.warn("Fail to close the rtsp unit. (id={})", rtspUnitId, e);
        } finally {
            rtspUnitMapLock.unlock();
        }
    }

    public void closeAllRtspUnits() {
        try {
            rtspUnitMapLock.lock();
            NettyChannelManager.getInstance().deleteAllRtcpChannels();
            NettyChannelManager.getInstance().deleteAllRtspChannels();
            rtspUnitMap.entrySet().removeIf(Objects::nonNull);
        } catch (Exception e) {
            logger.warn("Fail to close all rtsp units.", e);
        } finally {
            rtspUnitMapLock.unlock();
        }
    }

    public RtspUnit getRtspUnit(String rtspUnitId) {
        return rtspUnitMap.get(rtspUnitId);
    }

    public int getRtspUnitMapSize() {
        return rtspUnitMap.size();
    }

}
