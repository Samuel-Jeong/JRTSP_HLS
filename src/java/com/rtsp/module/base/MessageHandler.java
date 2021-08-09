package com.rtsp.module.base;

import com.rtsp.module.ImageTranslator;
import com.rtsp.module.MessageSender;
import com.rtsp.module.RtspManager;
import com.rtsp.module.VideoStream;
import com.rtsp.module.netty.NettyChannelManager;
import com.rtsp.protocol.RtpPacket;
import com.rtsp.service.TaskManager;
import com.rtsp.service.base.TaskUnit;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.jcodec.common.Format;
import org.jcodec.containers.mp4.MP4Packet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * @class public class MessageHandler extends TaskUnit
 * @brief MessageHandler class
 */
public class MessageHandler extends TaskUnit {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private final int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
    private final int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms

    private final String key;

    private final String listenIp;
    private final int listenPort;

    private final String remoteIp;
    private final int remotePort;

    int imageIndex = 0; //image nb of the image currently transmitted
    VideoStream video; //VideoStream object used to access video frames
    private int videoLength;

    ImageTranslator imageTranslator;

    ////////////////////////////////////////////////////////////////////////////////

    public MessageHandler(int interval, String key, String listenIp, int listenPort, String remoteIp, int remotePort, String fileName) {
        super(interval);

        this.key = key;

        this.listenIp = listenIp;
        this.listenPort = listenPort;

        this.remoteIp = remoteIp;
        this.remotePort = remotePort;

        this.imageTranslator = new ImageTranslator(0.8f);

        try {
            String rtspStr = fileName.substring(
                    fileName.lastIndexOf("rtsp:") + 8
            );

            fileName = rtspStr.substring(
                    rtspStr.indexOf("/")
            );

            if (fileName.charAt(fileName.length() - 1) == '/') {
                fileName = fileName.substring(
                        0,
                        fileName.length() - 1
                );
            }

            video = new VideoStream(
                    fileName,
                    Format.H264
            );
            videoLength = video.getFrameCount();
            logger.debug("fileName: {}, frameCount={}", fileName, videoLength);
        } catch (Exception e) {
            logger.warn("Fail to create the video stream. (fileName={})", fileName, e);
            video = null;
            TaskManager.getInstance().removeTask(
                    MessageSender.class.getSimpleName() + "_" + key
            );
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    @Override
    public void run() {
        try {
            MessageSender messageSender = NettyChannelManager.getInstance().getMessageSender(key, listenIp, listenPort);
            if (!messageSender.isActive()) {
                logger.warn("| MessageSender is not active or deleted. Send failed.");
                return;
            }

            // 모두 비디오 프레임 전송하면 자동 소멸
            if (imageIndex >= videoLength) {
                TaskManager.getInstance().removeTask(
                        MessageSender.class.getSimpleName() + "_" + key
                );
                return;
            }

            // 1) Get the next frame to send from the video
            byte[] data;
            if (video != null) {
                data = video.getNextFrame(imageIndex);
                if (data == null) {
                    return;
                }
            } else {
                return;
            }

            int imageLength = data.length;

            RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
            if (rtspUnit == null) {
                return;
            }

            // 2) Adjust quality of the image if there is congestion detected
            int congestionLevel = rtspUnit.getCongestionLevel();
            if (congestionLevel > 0) {
                imageTranslator.setCompressionQuality(1.0f - congestionLevel * 0.2f);
                byte[] frame = imageTranslator.compress(
                        Arrays.copyOfRange(
                                data,
                                0,
                                imageLength
                        )
                );

                imageLength = frame.length;
                System.arraycopy(frame, 0, data, 0, imageLength);
            }

            // 3) Builds an RtpPacket object containing the frame
            RtpPacket rtpPacket = new RtpPacket(
                    MJPEG_TYPE,
                    imageIndex,
                    imageIndex * FRAME_PERIOD,
                    data,
                    imageLength
            );

            // 4) Get to total length of the full rtp packet to send
            int packetLength = rtpPacket.getLength();

            // 5) Retrieve the packet
            byte[] packetBits = new byte[packetLength];
            rtpPacket.getPacket(packetBits);

            // 6) Send the packet
            ByteBuf buf = Unpooled.copiedBuffer(packetBits);
            messageSender.send(buf, remoteIp, remotePort);

            // 7) Print the packet
            logger.debug("Send frame #" + imageIndex + ", Frame size: " + imageLength + " (" + packetLength + ")");
            //logger.debug("{}", rtpPacket);

            imageIndex++;
        }
        catch(Exception e) {
            logger.warn("MessagesHandler.run.Exception", e);
        }
    }
}
