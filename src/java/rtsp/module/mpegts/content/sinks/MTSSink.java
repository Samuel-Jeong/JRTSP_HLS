package rtsp.module.mpegts.content.sinks;


import rtsp.module.mpegts.content.MpegTsPacket;

public interface MTSSink {

    void send(MpegTsPacket packet) throws Exception;

}
