package rtsp.module.mpegts.content;

import rtsp.module.mpegts.content.sinks.MTSSink;
import rtsp.module.mpegts.content.sinks.UDPTransport;
import rtsp.module.mpegts.content.sources.MTSSource;
import rtsp.module.mpegts.content.sources.MTSSources;
import rtsp.module.mpegts.content.sources.ResettableMTSSource;

import java.io.File;

public class MpegTsStreamerTest {
    public static void main(String[] args) throws Exception {

        // Set up mts sink
        MTSSink transport = UDPTransport.builder()
                .setAddress("127.0.0.1")
                .setPort(1234)
                .setSoTimeout(5000)
                .setTtl(1)
                .build();
        ResettableMTSSource ts1 = MTSSources.from(new File("Example"));

        // Build source
        MTSSource source = MTSSources.loop(ts1);

        // build streamer
        MpegTsStreamer streamer = MpegTsStreamer.builder()
                .setSource(source)
                .setSink(transport)
                .build();

        // Start streaming
        streamer.stream();

        ///////////////////////////////////////////////////////////////////////////
// [UDP] Set up packet source
                        /*MTSSource movie = MTSSources.from(new File(tsFileName));
                        MTSSink transport = UDPTransport.builder()
                                .setAddress(streamer.getDestIp()) // Can be a multicast address
                                .setPort(streamer.getDestPort())
                                .setSoTimeout(1000)
                                .setTtl(1)
                                .build();
                        MpegTsStreamer mpegTsStreamer = MpegTsStreamer.builder()
                                .setSource(movie)
                                .setSink(transport)
                                .build();
                        mpegTsStreamer.stream();*/
///////////////////////////////////////////////////////////////////////////


///////////////////////////////////////////////////////////////////////////
// [UDP] SLEEP
//logger.debug("({}) ({}) [SEND TS] (bitrate={}, {})", rtspUnit.getRtspUnitId(), streamer.getSessionId(), mediaSegment.bitrate(), mediaSegment);
                    /*long sec = (long) mediaSegment.duration();
                    long msec = (long) ((mediaSegment.duration() - sec) * 1000);
                    long timeout = sec * 1000 + msec;
                    logger.debug("({}) ({}) SLEEP: {}", rtspUnit.getRtspUnitId(), streamer.getSessionId(), timeout);
                    timeUnit.sleep(timeout);*/
///////////////////////////////////////////////////////////////////////////

    }
}
