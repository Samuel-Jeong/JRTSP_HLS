package com.rtsp.module;

import com.rtsp.ffmpeg.FfmpegManager;
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
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.channels.FileChannel;
import java.util.ArrayList;

public class VideoStream {

    private static final Logger logger = LoggerFactory.getLogger(VideoStream.class);

    private File file;
    private String tempJpgFilePath;
    private final String fileNameOnly;

    private final String resultJpgFilePath;
    private final String resultTsFilePath;

    private int curFrameCount = 0;
    private int frameCount = 0;
    private final ArrayList<Dimension> dimensionList = new ArrayList<>();

    ////////////////////////////////////////////////////////////////////////////////

    public VideoStream(String fileName) {
        file = new File(fileName);
        String fileTotalPath = fileName;

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
                fileTotalPath = newFileName;
            } catch (Exception e) {
                logger.warn("Fail to process the h264 file. (fileName={})", fileName, e);
            }
        }

        tempJpgFilePath = fileTotalPath.substring(
                0,
                fileTotalPath.lastIndexOf("/")
        );

        fileNameOnly = fileTotalPath.substring(
                fileTotalPath.lastIndexOf("/") + 1,
                fileTotalPath.lastIndexOf(".")
        );

        tempJpgFilePath += "/" + fileNameOnly + "_streaming/";
        File tempJpgFilePathFile = new File(tempJpgFilePath);
        if (tempJpgFilePathFile.mkdirs()) {
            logger.debug("Success to make the directory. ({})", tempJpgFilePathFile);
        }

        resultJpgFilePath = tempJpgFilePath + fileNameOnly + "_%d.jpg";
        resultTsFilePath = tempJpgFilePath + "hls/" + fileNameOnly + ".m3u8";

        if (frameCount == 0) {
            try {
                FrameGrab grab = FrameGrab.createFrameGrab(
                        NIOUtils.readableChannel(file)
                );

                Picture picture;
                while (null != (picture = grab.getNativeFrame())) {
                    //logger.debug(picture.getWidth() + "x" + picture.getHeight() + " " + picture.getColor());
                    frameCount++;
                    dimensionList.add(
                            new Dimension(
                                    picture.getWidth(),
                                    picture.getHeight()
                            )
                    );
                }
            } catch (Exception e) {
                logger.warn("Fail to get the frame count. (fileName={})", fileName, e);
            }
        }
    }

    public Dimension getDimension(int index) {
        return dimensionList.get(index);
    }

    public int getFrameCount() {
        return frameCount;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public byte[] getNextFrame(int frameIndex) {
        try {
            Picture picture = FrameGrab.getFrameAtSec(
                    file,
                    ((double) (frameIndex)) / 5000
            );

            /*Picture picture = FrameGrab.getFrameAtSec(
                    file,
                    frameIndex
            );*/

            /*ByteBuffer _out = ByteBuffer
                    .allocate(
                            picture.getSize().getWidth() *
                                    picture.getSize().getHeight()
                    );
            VideoEncoder.EncodedFrame encodedFrame = h264Encoder.encodeFrame(picture, _out);
            logger.debug("({}) TYPE: {}", frameIndex, encodedFrame.isKeyFrame());
            byte[] frame = encodedFrame.getData().array();*/

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            BufferedImage bufferedImage = AWTUtil.toBufferedImage(picture);

            saveJpg(bufferedImage);

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

    private void saveJpg(BufferedImage bufferedImage) {
        try {
            String jpgFileName = tempJpgFilePath + fileNameOnly + "_" + curFrameCount + ".jpg";
            File jpgFile = new File(jpgFileName);

            ImageIO.write(
                    bufferedImage,
                    "jpg",
                    jpgFile
            );

            if (jpgFile.exists()) {
                curFrameCount++;
                //logger.debug("Success to save the jpg file. (fileName={})", jpgFileName);
            }

            FfmpegManager.convertJpegsToM3u8(
                    resultJpgFilePath,
                    resultTsFilePath
            );
        } catch (Exception e) {
            logger.warn("Fail to save the jpg file. (tempJpgFilePath={})", tempJpgFilePath, e);
        }
    }

}