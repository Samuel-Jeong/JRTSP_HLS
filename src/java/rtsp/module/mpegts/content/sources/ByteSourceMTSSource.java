package rtsp.module.mpegts.content.sources;

import com.google.common.base.Preconditions;
import com.google.common.io.ByteSource;
import rtsp.module.mpegts.content.Constants;
import rtsp.module.mpegts.content.MTSPacket;

import java.io.InputStream;
import java.nio.ByteBuffer;

public class ByteSourceMTSSource extends AbstractMTSSource implements ResettableMTSSource {

    private final ByteSource byteSource;

    private InputStream stream;


    private ByteSourceMTSSource(ByteSource byteSource) {
        this.byteSource = byteSource;
    }

    public static ByteSourceMTSSourceBuilder builder() {
        return new ByteSourceMTSSourceBuilder();
    }

    @Override
    public void reset() throws Exception {
        if (stream != null) {
            try (InputStream ignored = stream) {
                //close
            }
        }
        stream = byteSource.openBufferedStream();
    }

    @Override
    protected MTSPacket nextPacketInternal() throws Exception {
        if (stream == null) {
            stream = byteSource.openBufferedStream();
        }

        byte[] barray = new byte[Constants.MPEGTS_PACKET_SIZE];
        if (stream.read(barray) != Constants.MPEGTS_PACKET_SIZE) {
            stream.close();
            return null;
        }

        // Parse the packet
        return new MTSPacket(ByteBuffer.wrap(barray));
    }

    @Override
    protected void closeInternal() throws Exception {
        if (stream != null) {
            try (InputStream ignored = stream) {
                //close
            }
        }
    }

    public static class ByteSourceMTSSourceBuilder {
        private ByteSource byteSource;

        private ByteSourceMTSSourceBuilder() {
        }

        public ByteSourceMTSSource build() {
            Preconditions.checkNotNull(byteSource);
            return new ByteSourceMTSSource(byteSource);
        }

        public ByteSourceMTSSourceBuilder setByteSource(ByteSource byteSource) {
            this.byteSource = byteSource;
            return this;
        }
    }
}
