package rtsp.protocol.rtcp.module;

import java.util.concurrent.TimeUnit;

/**
 * @author kulikov
 */
public interface Clock {

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    long getTime();

    long getCurrentTime();

    long getTime(TimeUnit timeUnit);

    TimeUnit getTimeUnit();
    ////////////////////////////////////////////////////////////

}