package com.rtsp.module;

import com.rtsp.ffmpeg.FfmpegManager;
import com.rtsp.module.base.JpgInfo;
import com.rtsp.service.TaskManager;
import com.rtsp.service.base.ConcurrentCyclicFIFO;
import com.rtsp.service.base.TaskUnit;
import org.jcodec.api.FrameGrab;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.model.Picture;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
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
import java.util.concurrent.atomic.AtomicLong;

public class VideoStream {

    private static final Logger logger = LoggerFactory.getLogger(VideoStream.class);

    private File file;
    private String tempJpgFilePath;
    private final String fileNameOnly;

    private final String resultJpgFilePath;
    private final String resultTsFilePath;

    private final AtomicLong curFrameCount = new AtomicLong(0);
    private double curTime = 0;
    private double totalTime = 0;
    //private int curFrameCount = 0;
    //private int totalFrameCount = 0;

    public static final double FRAME_RATE = 0.1;

    private Thread jpgThread = null;
    private final ConcurrentCyclicFIFO<JpgInfo> buffer = new ConcurrentCyclicFIFO<>();

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

        if (totalTime == 0) {
            try {
                FileChannelWrapper fileChannelWrapper = NIOUtils.readableChannel(file);
                MP4Demuxer deMuxer = MP4Demuxer.createMP4Demuxer(fileChannelWrapper);
                DemuxerTrack video_track = deMuxer.getVideoTrack();
                totalTime = video_track.getMeta().getTotalDuration();
                logger.debug("Total duration: {}", totalTime);
            } catch (Exception e) {
                logger.warn("Fail to get the frame count. (fileName={})", fileName, e);
            }
        }
    }

    public VideoStream start() {
        /*if (jpgThread == null) {
            jpgThread = new Thread(
                    new JpgManager(buffer)
            );
            jpgThread.start();
        }*/

        return this;
    }

    public void stop() {
        /*if (jpgThread != null) {
            jpgThread.interrupt();
        }*/
    }

    public double getTotalFrameCount() {
        return totalTime;
    }

    ////////////////////////////////////////////////////////////////////////////////

    public byte[] getNextFrame() {
        try {
            Picture picture = FrameGrab.getFrameAtSec(
                    file,
                    FRAME_RATE
            );

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

            //
            long frameCount = getCurFrameCount();
            saveJpg(bufferedImage,
                    tempJpgFilePath,
                    fileNameOnly,
                    resultJpgFilePath,
                    resultTsFilePath,
                    curTime,
                    totalTime,
                    frameCount
            );

            /*buffer.offer(
                    new JpgInfo(
                            bufferedImage,
                            tempJpgFilePath,
                            fileNameOnly,
                            resultJpgFilePath,
                            resultTsFilePath,
                            curTime,
                            totalTime,
                            frameCount
                    )
            );*/
            //

            ImageIO.write(
                    bufferedImage,
                    "jpg",
                    byteArrayOutputStream
            );
            byteArrayOutputStream.flush();

            byte[] frame = byteArrayOutputStream.toByteArray();
            byteArrayOutputStream.close();

            curTime += FRAME_RATE;
            curFrameCount.set(frameCount + 1);

            return frame;
        } catch (Exception e) {
            // Ignored
            return null;
        }
    }

    public long getCurFrameCount() {
        return curFrameCount.get();
    }

    private void saveJpg(BufferedImage bufferedImage, String tempJpgFilePath, String fileNameOnly, String resultJpgFilePath, String resultTsFilePath, double curTime, double totalTime, long curFrameCount) {
        try {
            String jpgFileName = tempJpgFilePath + fileNameOnly + "_" + curFrameCount + ".jpg";
            File jpgFile = new File(jpgFileName);

            ImageIO.write(
                    bufferedImage,
                    "jpg",
                    jpgFile
            );

            if ((curFrameCount % 30 == 0)
                    || (curTime >= totalTime)) {
                FfmpegManager.convertJpegsToM3u8(
                        curFrameCount,
                        resultJpgFilePath,
                        resultTsFilePath
                );
            }
        } catch (Exception e) {
            logger.warn("Fail to save the jpg file. (tempJpgFilePath={})", tempJpgFilePath, e);
        }
    }

    ////////////////////////////////////////////////////////////////////////////////

    private static class JpgManager implements Runnable {

        private final ConcurrentCyclicFIFO<JpgInfo> buffer;
        private int loopIndex = 0;

        protected JpgManager(ConcurrentCyclicFIFO<JpgInfo> buffer) {
            this.buffer = buffer;
        }

        @Override
        public void run() {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    JpgInfo jpgInfo = buffer.poll();
                    if (jpgInfo == null) {
                        continue;
                    }

                    saveJpg(
                            jpgInfo.getBufferedImage(),
                            jpgInfo.getTempJpgFilePath(),
                            jpgInfo.getFileNameOnly(),
                            jpgInfo.getResultJpgFilePath(),
                            jpgInfo.getResultTsFilePath(),
                            jpgInfo.getCurTime(),
                            jpgInfo.getTotalTime(),
                            jpgInfo.getCurFrameCount()
                    );
                } catch (Exception e) {
                    logger.warn("JpgManager.run.exception", e);
                }
            }
        }

        private void saveJpg(BufferedImage bufferedImage, String tempJpgFilePath, String fileNameOnly, String resultJpgFilePath, String resultTsFilePath, double curTime, double totalTime, long curFrameCount) {
            try {
                String jpgFileName = tempJpgFilePath + fileNameOnly + "_" + curFrameCount + ".jpg";
                File jpgFile = new File(jpgFileName);

                ImageIO.write(
                        bufferedImage,
                        "jpg",
                        jpgFile
                );

                if ((loopIndex % 30 == 0)
                        || (curTime >= totalTime)) {
                    FfmpegManager.convertJpegsToM3u8(
                            curFrameCount,
                            resultJpgFilePath,
                            resultTsFilePath
                    );
                    loopIndex = 0;
                } else {
                    loopIndex++;
                }
            } catch (Exception e) {
                logger.warn("Fail to save the jpg file. (tempJpgFilePath={})", tempJpgFilePath, e);
            }
        }
    }

}