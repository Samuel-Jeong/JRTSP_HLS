package com.rtsp.service;

import com.fsm.module.StateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rtsp.config.ConfigManager;
import com.rtsp.fsm.RtspEvent;
import com.rtsp.fsm.RtspState;
import com.rtsp.module.RtspManager;
import com.rtsp.module.base.RtspUnit;
import com.rtsp.module.netty.NettyChannelManager;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * @class public class ServiceManager
 * @brief Voip Phone 의 전체 Service 관리 클래스
 */
public class ServiceManager {

    private static final Logger logger = LoggerFactory.getLogger(ServiceManager.class);

    private static ServiceManager serviceManager = null;

    private String externalClientRtspUnitId = null;

    private static final int DELAY = 1000;

    private boolean isQuit = false;

    ////////////////////////////////////////////////////////////////////////////////

    public ServiceManager() {
        Runtime.getRuntime().addShutdownHook(new ShutDownHookHandler("ShutDownHookHandler", Thread.currentThread()));

    }

    public static ServiceManager getInstance ( ) {
        if (serviceManager == null) {
            serviceManager = new ServiceManager();
        }

        return serviceManager;
    }

    ////////////////////////////////////////////////////////////////////////////////

    private boolean start () {
        ConfigManager configManager = AppInstance.getInstance().getConfigManager();
        if (configManager.isExternalClientAccess()) {
            externalClientRtspUnitId = UUID.randomUUID().toString();
            RtspManager.getInstance().openRtspUnit(
                    externalClientRtspUnitId,
                    configManager.getLocalListenIp(),
                    configManager.getLocalRtspListenPort()
            );

            RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit(externalClientRtspUnitId);
            if (rtspUnit == null) {
                logger.warn("Fail to create the external client's rtsp unit.");
                return false;
            }

            StateHandler rtspStateHandler = rtspUnit.getStateManager().getStateHandler(RtspState.NAME);
            rtspStateHandler.fire(
                    RtspEvent.REGISTER,
                    rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
            );


        }

        ResourceManager.getInstance().initResource();
        NettyChannelManager.getInstance().addRegisterChannel();

        logger.debug("| All services are opened.");
        return true;
    }

    public void stop () {
        NettyChannelManager.getInstance().removeRegisterChannel();
        NettyChannelManager.getInstance().stop();
        RtspManager.getInstance().closeAllRtspUnits();
        ResourceManager.getInstance().releaseResource();

        isQuit = true;
        logger.debug("| All services are closed.");
    }

    /**
     * @fn public void loop ()
     * @brief Main Service Loop
     */
    public void loop () {
        if (!start()) {
            logger.error("Fail to start the program.");
            return;
        }

        TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        while (!isQuit) {
            try {
                timeUnit.sleep(DELAY);
            } catch (InterruptedException e) {
                logger.warn("| ServiceManager.loop.InterruptedException", e);
            }
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    public String getExternalClientRtspUnitId() {
        return externalClientRtspUnitId;
    }

    public void setExternalClientRtspUnitId(String externalClientRtspUnitId) {
        this.externalClientRtspUnitId = externalClientRtspUnitId;
    }

    ////////////////////////////////////////////////////////////////////////////////

    /**
     * @class private static class ShutDownHookHandler extends Thread
     * @brief Graceful Shutdown 을 처리하는 클래스
     * Runtime.getRuntime().addShutdownHook(*) 에서 사용됨
     */
    private static class ShutDownHookHandler extends Thread {

        // shutdown 로직 후에 join 할 thread
        private final Thread target;

        public ShutDownHookHandler (String name, Thread target) {
            super(name);

            this.target = target;
            logger.debug("| ShutDownHookHandler is initiated. (target={})", target.getName());
        }

        /**
         * @fn public void run ()
         * @brief 정의된 Shutdown 로직을 수행하는 함수
         */
        @Override
        public void run ( ) {
            try {
                shutDown();
                target.join();
                logger.debug("| ShutDownHookHandler's target is finished successfully. (target={})", target.getName());
            } catch (Exception e) {
                logger.warn("| ShutDownHookHandler.run.Exception", e);
            }
        }

        /**
         * @fn private void shutDown ()
         * @brief Runtime 에서 선언된 Handler 에서 사용할 서비스 중지 함수
         */
        private void shutDown ( ) {
            logger.warn("| Process is about to quit. (Ctrl+C)");
            ServiceManager.getInstance().stop();
        }
    }

}
