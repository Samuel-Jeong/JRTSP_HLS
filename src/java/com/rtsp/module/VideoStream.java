package com.rtsp.module;

import com.rtsp.module.base.RtspUnit;
import com.rtsp.mpegts.NioUtils;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

public class VideoStream {

    private static final Logger logger = LoggerFactory.getLogger(VideoStream.class);

    private final String fileName;
    private final String resultM3U8FilePath;

    private double totalTime = 0;

    ////////////////////////////////////////////////////////////////////////////////

    public VideoStream(String fileName) {
        fileName = RtspUnit.getPureFileName(fileName);

        if (fileName.endsWith(".m3u8")) {
            fileName = fileName.substring(
                    0,
                    fileName.lastIndexOf(".")
            );
            fileName += ".mp4";
        }

        logger.debug("fileName={}", fileName);

        File file = new File(fileName);
        String fileTotalPath = fileName;
        this.fileName = fileName;

        String tempFilePath = fileTotalPath.substring(
                0,
                fileTotalPath.lastIndexOf("/")
        );

        String fileNameOnly = fileTotalPath.substring(
                fileTotalPath.lastIndexOf("/") + 1,
                fileTotalPath.lastIndexOf(".")
        );

        resultM3U8FilePath = tempFilePath + File.separator + fileNameOnly + ".m3u8";
        logger.debug("fileName={}, resultM3U8FilePath={}", fileName, resultM3U8FilePath);

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

    public String getFileName() {
        return fileName;
    }

    public String getResultM3U8FilePath() {
        return resultM3U8FilePath;
    }

    public double getTotalFrameCount() {
        return totalTime;
    }

}