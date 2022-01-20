package rtsp.module.mpegts.content.sources;

import rtsp.module.mpegts.content.MTSPacket;

public interface MTSSource {
    MTSPacket nextPacket() throws Exception;

    void close() throws Exception;
}
