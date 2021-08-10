package com.rtsp.module.base;

import com.rtsp.media.Packetizer;
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

    int imageIndex = 0; //image nb of the image currently transmitted
    VideoStream video; //VideoStream object used to access video frames
    private int videoLength;

    ImageTranslator imageTranslator;

    Packetizer packetizer = new Packetizer();

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
                logger.warn("| MessageSender is not active or deleted. (key={}, listenIp={}, listenPort={})", key, listenIp, listenPort);
                TaskManager.getInstance().removeTask(
                        MessageSender.class.getSimpleName() + "_" + key
                );
                return;
            }

            // 모두 비디오 프레임 전송하면 자동 소멸
            if (imageIndex >= videoLength) {
                TaskManager.getInstance().removeTask(
                        MessageSender.class.getSimpleName() + "_" + key
                );
                return;
            }

            //
            byte[] data;
            if (video != null) {
                data = video.getNextFrame(imageIndex);
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

            //
            /*VideoFormat jpegVideoFormat = new VideoFormat(
                    VideoFormat.JPEG,
                    video.getDimension(imageIndex),
                    data.length,
                    Format.byteArray,
                    90000.0f
            );

            VideoFormat jpegRtpVideoFormat = new VideoFormat(
                    VideoFormat.JPEG_RTP
            );

            Buffer inBuffer = new Buffer();
            inBuffer.setFormat(jpegVideoFormat);
            inBuffer.setLength(data.length);
            inBuffer.setData(data);
            inBuffer.setSequenceNumber(imageIndex);
            inBuffer.setTimeStamp((long) imageIndex * FRAME_PERIOD);

            Buffer outBuffer = new Buffer();
            outBuffer.setFormat(jpegRtpVideoFormat);

            packetizer.setInputFormat(jpegVideoFormat);
            packetizer.setOutputFormat(jpegRtpVideoFormat);
            packetizer.open();
            for (;;) {
                int result = packetizer.process(inBuffer, outBuffer);
                *//*switch (result) {
                    case 0:
                        logger.trace("BUFFER_PROCESSED_OK");
                        break;
                    case 1:
                        logger.trace("BUFFER_PROCESSED_FAILED");
                        break;
                    case 2:
                        logger.trace("INPUT_BUFFER_NOT_CONSUMED");
                        break;
                    case 3:
                        logger.trace("OUTPUT_BUFFER_NOT_FILLED");
                        break;
                    case 4:
                        logger.trace("PLUGIN_TERMINATED");
                        break;
                    default:
                        break;
                }*//*

                RtpPacket rtpPacket = new RtpPacket();
                int marker = 0;
                if (result == BUFFER_PROCESSED_OK) {
                    marker = 1;
                }

                rtpPacket.setValue(
                        2, 0, 0, 0, marker, MJPEG_TYPE,
                        outBuffer.getSequenceNumber(),
                        outBuffer.getTimeStamp(),
                        rtspUnit.getSsrc(),
                        (byte[]) outBuffer.getData(),
                        outBuffer.getLength()
                );

                // Send the packet
                byte[] totalData = rtpPacket.getData();
                ByteBuf buf = Unpooled.copiedBuffer(totalData);
                messageSender.send(buf, remoteIp, remotePort);
                //logger.trace("\t {#{}} ({})", curSeqNum, outBuffer.getLength());

                if (result == BUFFER_PROCESSED_OK) {
                    break;
                }
            }
            packetizer.close();*/

            RtpPacket rtpPacket = new RtpPacket();
            rtpPacket.setValue(
                    2, 0, 0, 0, 0, MJPEG_TYPE, imageIndex,
                    (long) imageIndex * FRAME_PERIOD,
                    rtspUnit.getSsrc(),
                    data,
                    data.length
            );

            // Send the packet
            byte[] totalData = rtpPacket.getData();
            ByteBuf buf = Unpooled.copiedBuffer(totalData);
            messageSender.send(buf, remoteIp, remotePort);

            logger.debug(">> Frame[#{}] (size={})", imageIndex, data.length);
            //

            imageIndex++;
        }
        catch(Exception e) {
            logger.warn("MessagesHandler.run.Exception", e);
        }
    }
}
