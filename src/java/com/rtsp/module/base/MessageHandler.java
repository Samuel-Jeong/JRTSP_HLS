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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

import static com.rtsp.module.VideoStream.FRAME_RATE;

/**
 * @class public class MessageHandler extends TaskUnit
 * @brief MessageHandler class
 */
public class MessageHandler extends TaskUnit {

    private static final Logger logger = LoggerFactory.getLogger(MessageHandler.class);

    private static final int MJPEG_TYPE = 26; //RTP payload type for MJPEG video
    private static final int FRAME_PERIOD = 100; //Frame period of the video to stream, in ms

    private final String key;

    private final String listenIp;
    private final int listenPort;

    private final String remoteIp;
    private final int remotePort;

    private VideoStream video; //VideoStream object used to access video frames
    private double curTime = 0;
    private double totalTime;

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
                    fileName
            ).start();

            totalTime = video.getTotalFrameCount();

            logger.debug("fileName: {}, frameCount={}", fileName, totalTime);
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
                logger.warn("| MessageSender is not active or deleted. (key={}, listenIp={}, listenPort={})", key, listenIp, listenPort);
                TaskManager.getInstance().removeTask(
                        MessageSender.class.getSimpleName() + "_" + key
                );
                return;
            }

            // 모두 비디오 프레임 전송하면 자동 소멸
            if (curTime >= totalTime) {
                video.stop();
                TaskManager.getInstance().removeTask(
                        MessageSender.class.getSimpleName() + "_" + key
                );
                return;
            }

            //
            byte[] data;
            if (video != null) {
                data = video.getNextFrame();
                if (data == null) {
                    return;
                }
            } else {
                return;
            }
            //

            //
            RtspUnit rtspUnit = RtspManager.getInstance().getRtspUnit();
            if (rtspUnit == null) {
                return;
            }

            int imageLength = data.length;
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
            //

            long curFrameCount = video.getCurFrameCount();
            RtpPacket rtpPacket = new RtpPacket();
            rtpPacket.setValue(
                    2, 0, 0, 0, 0, MJPEG_TYPE, curFrameCount,
                    curFrameCount * FRAME_PERIOD,
                    rtspUnit.getSsrc(),
                    data,
                    data.length
            );

            // Send the packet
            byte[] totalData = rtpPacket.getData();
            ByteBuf buf = Unpooled.copiedBuffer(totalData);
            messageSender.send(buf, remoteIp, remotePort);

            logger.debug(">> Frame[#{}] (size={})", curFrameCount, data.length);
            //

            curTime += FRAME_RATE;
        }
        catch(Exception e) {
            logger.warn("MessagesHandler.run.Exception", e);
        }
    }
}
