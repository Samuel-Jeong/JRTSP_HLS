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

    }
}
