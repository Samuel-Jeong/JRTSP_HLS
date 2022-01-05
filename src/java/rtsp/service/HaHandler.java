package rtsp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.module.RtspManager;
import rtsp.module.netty.NettyChannelManager;
import rtsp.service.scheduler.job.Job;
import rtsp.system.SystemManager;

import java.util.concurrent.TimeUnit;

/**
 * @author jamesj
 * @class public class ServiceHaHandler extends TaskUnit
 * @brief ServiceHaHandler
 */
public class HaHandler extends Job {

    private static final Logger logger = LoggerFactory.getLogger(HaHandler.class);

    private final NettyChannelManager nettyChannelManager = NettyChannelManager.getInstance();

    ////////////////////////////////////////////////////////////////////////////////

    public HaHandler(String name, int initialDelay, int interval, TimeUnit timeUnit, int priority, int totalRunCount, boolean isLasted) {
        super(name, initialDelay, interval, timeUnit, priority, totalRunCount, isLasted);
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void run () {
        SystemManager systemManager = SystemManager.getInstance();

        String cpuUsageStr = systemManager.getCpuUsage();
        String memoryUsageStr = systemManager.getHeapMemoryUsage();

        logger.debug("| cpu=[{}], mem=[{}], thread=[{}] | RtspUnitCount=[{}]",
                cpuUsageStr, memoryUsageStr, Thread.activeCount(),
                RtspManager.getInstance().getRtspUnitMapSize()
        );
    }

}
