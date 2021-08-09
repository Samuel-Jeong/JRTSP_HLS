package com.rtsp.module;

import org.jcodec.api.FrameGrab;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.mp4parser.Container;
import org.mp4parser.muxer.FileDataSourceImpl;
import org.mp4parser.muxer.Movie;
import org.mp4parser.muxer.builder.DefaultMp4Builder;
import org.mp4parser.muxer.tracks.h264.H264TrackImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.channels.FileChannel;

public class VideoStream {

    private static final Logger logger = LoggerFactory.getLogger(VideoStream.class);

    private File file;
    private int frameCount = 0;

    ////////////////////////////////////////////////////////////////////////////////

    public VideoStream(String fileName) {
        file = new File(fileName);

        if (fileName.endsWith(".h264")) {
            try {
                String newFileName = fileName.substring(
                        0,
                        fileName.lastIndexOf(".")
                );
                newFileName += ".mp4";

                H264TrackImpl h264Track = new H264TrackImpl(
                        new FileDataSourceImpl(fileName)
                );

                Movie movie = new Movie();
                movie.addTrack(h264Track);

                Container mp4file = new DefaultMp4Builder().build(movie);
                FileChannel fc = new FileOutputStream(newFileName).getChannel();
                mp4file.writeContainer(fc);
                fc.close();

                file = new File(newFileName);
            } catch (Exception e) {
                logger.warn("Fail to process the h264 file. (fileName={})", fileName, e);
            }
        }

        if (frameCount == 0) {
            try {
                FrameGrab grab = FrameGrab.createFrameGrab(
                        NIOUtils.readableChannel(file)
                );

                Picture picture;
                while (null != (picture = grab.getNativeFrame())) {
                    //logger.debug(picture.getWidth() + "x" + picture.getHeight() + " " + picture.getColor());
                    frameCount++;
                }
            } catch (Exception e) {
                logger.warn("Fail to get the frame count. (fileName={})", fileName, e);
            }
        }
    }

    public int getFrameCount() {
        return frameCount;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public byte[] getNextFrame(int frameIndex) {
        try {
            Picture picture = FrameGrab.getFrameFromFile(
                    file,
                    frameIndex
            );

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);
            ImageIO.write(
                    bufferedImage,
                    "jpg",
                    byteArrayOutputStream
            );
            byteArrayOutputStream.flush();

            byte[] frame = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();
            return frame;
        } catch (Exception e) {
            // Ignored
            return null;
        }
    }

}