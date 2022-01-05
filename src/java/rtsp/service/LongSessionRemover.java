package rtsp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.module.RtspManager;
import rtsp.module.base.RtspUnit;
import rtsp.service.scheduler.job.Job;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class LongSessionRemover extends Job {

    private static final Logger logger = LoggerFactory.getLogger(LongSessionRemover.class);
    
    private final long limitTime;

    public LongSessionRemover(String name, int initialDelay, int interval, TimeUnit timeUnit, int priority, int totalRunCount, boolean isLasted) {
        super(name, initialDelay, interval, timeUnit, priority, totalRunCount, isLasted);

        limitTime = AppInstance.getInstance().getConfigManager().getLocalSessionLimitTime();
    }

    @Override
    public void run() {
        HashMap<String, RtspUnit> rtspUnitMap = RtspManager.getInstance().getCloneRtspMap();
        if (!rtspUnitMap.isEmpty()) {
            for (Map.Entry<String, RtspUnit> entry : rtspUnitMap.entrySet()) {
                if (entry == null) {
                    continue;
                }

                RtspUnit rtspUnit = entry.getValue();
                if (rtspUnit == null) {
                    continue;
                }

                long curTime = System.currentTimeMillis();
                if ((curTime - rtspUnit.getInitiationTime()) >= limitTime) {
                    RtspManager.getInstance().closeRtspUnit(rtspUnit.getRtspUnitId());
                    logger.warn("({}) REMOVED LONG SESSION(RtspUnit=\n{})", getName(), rtspUnit);
                }
            }
        }
    }
    
}
