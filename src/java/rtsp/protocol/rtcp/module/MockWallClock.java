package rtsp.protocol.rtcp.module;

import java.util.concurrent.TimeUnit;

/**
 * @author kulikov
 */
public class MockWallClock implements Clock {

    private final TimeUnit unit = TimeUnit.NANOSECONDS;
    private long time = System.nanoTime();
    private long currTime = System.currentTimeMillis();

    public long getTime() {
        return time;
    }

    public long getTime(TimeUnit timeUnit) {
        return timeUnit.convert(time, unit);
    }

    public TimeUnit getTimeUnit() {
        return unit;
    }

    public void tick(long amount) {
        time += amount;
        currTime += TimeUnit.NANOSECONDS.toMillis(amount);
    }

    public long getCurrentTime() {
        return currTime;
    }

}