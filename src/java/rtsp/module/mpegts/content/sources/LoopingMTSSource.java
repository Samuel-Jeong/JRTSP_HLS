package rtsp.module.mpegts.content.sources;

import rtsp.module.mpegts.content.MpegTsPacket;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

public class LoopingMTSSource extends AbstractMTSSource {
    private final ResettableMTSSource source;
    private final boolean fixContinuity;
    private final Integer maxLoops;
    private long currentLoop;

    public LoopingMTSSource(ResettableMTSSource source, boolean fixContinuity, Integer maxLoops) {
        this.source = source;
        this.fixContinuity = fixContinuity;
        this.maxLoops = maxLoops;
        currentLoop = 1;
    }

    public static LoopingMTSSourceBuilder builder() {
        return new LoopingMTSSourceBuilder();
    }

    @Override
    protected MpegTsPacket nextPacketInternal() throws Exception {
        MpegTsPacket packet = source.nextPacket();
        if (packet == null) {
            currentLoop++;
            if (maxLoops == null || (currentLoop <= maxLoops)) {
                source.reset();
                packet = source.nextPacket();
            }
        }
        return packet;
    }

    @Override
    protected void closeInternal() throws Exception {
        source.close();
    }

    public static class LoopingMTSSourceBuilder {
        private ResettableMTSSource source;
        private boolean fixContinuity;
        private Integer maxLoops;

        private LoopingMTSSourceBuilder() {
        }

        public LoopingMTSSource build() {
            checkNotNull(source);
            checkArgument(maxLoops == null || maxLoops > 0);
            return new LoopingMTSSource(source, fixContinuity, maxLoops);
        }

        public LoopingMTSSourceBuilder setSource(ResettableMTSSource source) {
            this.source = source;
            return this;
        }

        public LoopingMTSSourceBuilder setFixContinuity(boolean fixContinuity) {
            this.fixContinuity = fixContinuity;
            return this;
        }

        public LoopingMTSSourceBuilder setMaxLoops(Integer maxLoops) {
            this.maxLoops = maxLoops;
            return this;
        }
    }
}
