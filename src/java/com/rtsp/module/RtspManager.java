package com.rtsp.module;

import com.rtsp.fsm.RtspState;
import com.rtsp.module.base.RtspUnit;
import com.rtsp.module.netty.NettyChannelManager;
import com.rtsp.service.ResourceManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @class public class RtspManager
 * @brief RtspManager class
 */
public class RtspManager {

    public static final String RTSP_RES_SESSION = "Session";
    private static final Logger logger = LoggerFactory.getLogger(RtspManager.class);
    private static RtspManager rtspManager = null;

    private final HashMap<String, RtspUnit> rtspUnitMap = new HashMap<>();
    private final ReentrantLock rtspUnitMapLock = new ReentrantLock();

    ////////////////////////////////////////////////////////////////////////////////

    public RtspManager() {
        // Nothing
    }

    public static RtspManager getInstance() {
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
                    RtspState.INIT,
                    null
            );

            rtspUnitMap.putIfAbsent(rtspUnitId, rtspUnit);
        } catch (Exception e) {
            logger.warn("Fail to open the rtsp unit. (id={}, ip={}, port={})", rtspUnitId, ip, port, e);
        } finally {
            rtspUnitMapLock.unlock();
        }
    }

    public void closeRtspUnit(String rtspUnitId) {
        try {
            rtspUnitMapLock.lock();

            RtspUnit rtspUnit = getRtspUnit(rtspUnitId);
            NettyChannelManager.getInstance().deleteRtspChannel(
                    rtspUnit.getRtspChannel().getListenIp()
                            + "_" +
                            rtspUnit.getRtspChannel().getListenPort()
            );

            NettyChannelManager.getInstance().deleteRtcpChannel(
                    rtspUnit.getRtcpChannel().getListenIp()
                            + "_" +
                            rtspUnit.getRtcpChannel().getListenPort()
            );

            int port = rtspUnit.getServerPort();
            if (port > 0) {
                ResourceManager.getInstance().restorePort(port);
            }
        } catch (Exception e) {
            logger.warn("Fail to close the rtsp unit. (id={})", rtspUnitId, e);
        } finally {
            rtspUnitMapLock.unlock();
        }
    }

    public void closeAllRtspUnits() {
        try {
            rtspUnitMapLock.lock();

            for (Map.Entry<String, RtspUnit> entry : rtspUnitMap.entrySet()) {
                if (entry == null) {
                    return;
                }

                RtspUnit rtspUnit = entry.getValue();
                if (rtspUnit == null) {
                    return;
                }

                NettyChannelManager.getInstance().deleteRtspChannel(
                        rtspUnit.getRtspChannel().getListenIp()
                                + "_" +
                                rtspUnit.getRtspChannel().getListenPort()
                );

                NettyChannelManager.getInstance().deleteRtcpChannel(
                        rtspUnit.getRtcpChannel().getListenIp()
                                + "_" +
                                rtspUnit.getRtcpChannel().getListenPort()
                );

                int port = rtspUnit.getServerPort();
                if (port > 0) {
                    ResourceManager.getInstance().restorePort(port);
                }
            }
        } catch (Exception e) {
            logger.warn("Fail to close all rtsp units.", e);
        } finally {
            rtspUnitMapLock.unlock();
        }
    }

    public RtspUnit getRtspUnit(String rtspUnitId) {
        return rtspUnitMap.get(rtspUnitId);
    }

}
