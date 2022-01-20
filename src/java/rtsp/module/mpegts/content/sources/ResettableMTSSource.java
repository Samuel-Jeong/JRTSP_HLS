package rtsp.module.mpegts.content.sources;

public interface ResettableMTSSource extends MTSSource {
    void reset() throws Exception;
}
