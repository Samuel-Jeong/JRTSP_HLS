package rtsp.module.mpegts.content.sources;

import rtsp.module.mpegts.content.MpegTsPacket;

public abstract class AbstractMTSSource implements MTSSource {
    private boolean closed;

    @Override
    public final MpegTsPacket nextPacket() throws Exception {
        if (closed) {
            throw new IllegalStateException("Source is closed");
        }
        return nextPacketInternal();
    }

    @Override
    public final void close() throws Exception {
        try {
            closeInternal();
        } finally {
            closed = true;
        }
    }

    protected boolean isClosed() {
        return closed;
    }

    protected abstract MpegTsPacket nextPacketInternal() throws Exception;

    protected abstract void closeInternal() throws Exception;


    protected void finalize() throws Exception {
        if (!closed) {
            close();
        }
    }
}
