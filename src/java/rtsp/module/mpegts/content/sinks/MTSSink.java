package rtsp.module.mpegts.content.sinks;


import rtsp.module.mpegts.content.MTSPacket;

public interface MTSSink {

    void send(MTSPacket packet) throws Exception;

}
