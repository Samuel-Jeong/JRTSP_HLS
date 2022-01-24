package rtsp.module.mpegts.content.sources;

import rtsp.module.mpegts.content.MpegTsPacket;

public interface MTSSource {
    MpegTsPacket nextPacket() throws Exception;

    void close() throws Exception;
}
