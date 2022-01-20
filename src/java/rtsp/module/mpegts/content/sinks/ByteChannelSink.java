package rtsp.module.mpegts.content.sinks;

import rtsp.module.mpegts.content.MTSPacket;

import java.nio.channels.ByteChannel;

public class ByteChannelSink implements MTSSink {

    private final ByteChannel byteChannel;

    private ByteChannelSink(ByteChannel byteChannel) {
        this.byteChannel = byteChannel;
    }

    public static ByteChannelSinkBuilder builder() {
        return new ByteChannelSinkBuilder();
    }

    @Override
    public void send(MTSPacket packet) throws Exception {
        byteChannel.write(packet.getBuffer());
    }

    public static class ByteChannelSinkBuilder {
        private ByteChannel byteChannel;

        private ByteChannelSinkBuilder() {
        }

        public ByteChannelSink build() {
            return new ByteChannelSink(byteChannel);
        }

        public ByteChannelSinkBuilder setByteChannel(ByteChannel byteChannel) {
            this.byteChannel = byteChannel;
            return this;
        }
    }
}
