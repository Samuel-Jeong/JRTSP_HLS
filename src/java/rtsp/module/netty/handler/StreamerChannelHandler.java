package rtsp.module.netty.handler;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.module.RtspManager;
import rtsp.module.Streamer;
import rtsp.module.base.RtspUnit;
import rtsp.module.netty.NettyChannelManager;

/**
 * @class public class StreamerChannelHandler extends ChannelInboundHandlerAdapter
 * @brief StreamerChannelHandler class
 */
public class StreamerChannelHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(StreamerChannelHandler.class);

    private final String rtspUnitId;
    private final String sessionId;

    ////////////////////////////////////////////////////////////////////////////////

    public StreamerChannelHandler(String rtspUnitId, String sessionId) {
        this.rtspUnitId = rtspUnitId;
        this.sessionId = sessionId;
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        // Nothing
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        //logger.warn("({}) StreamerChannelHandler is inactive.", id);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        String causeString = cause.toString();
        logger.warn("({}) ({}) StreamerChannelHandler.Exception (cause={})", rtspUnitId, sessionId, causeString);

        if (causeString.contains("PortUnreachable")) {
            RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit(rtspUnitId);
            if (rtspUnit == null) { return; }

            Streamer streamer = rtspUnit.getStreamer();
            if (streamer == null) { return; }

            logger.debug("({}) ({}) Stop the streaming by [PortUnreachableException].", rtspUnit.getRtspUnitId(), streamer.getSessionId());
            NettyChannelManager.getInstance().stopStreaming(
                    rtspUnitId,
                    streamer.getSessionId(),
                    rtspUnit.getRtspListenIp(),
                    rtspUnit.getRtspListenPort()
            );

            NettyChannelManager.getInstance().deleteStreamer(rtspUnitId, streamer.getSessionId(), rtspUnit.getRtspListenIp(), rtspUnit.getRtspListenPort());
            rtspUnit.setStreamer(null);
            logger.debug("({}) ({}) Finish to stream the media by [PortUnreachableException].", rtspUnit.getRtspUnitId(), streamer.getSessionId());
        }

        ctx.close();
    }

}