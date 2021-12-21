package com.rtsp.module;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rtsp.module.base.RtspUnit;

import java.io.File;

/**
 * @class public class VideoStream
 * @brief VideoStream class
 * URI 가 M3U8 이어야하고, 스트리밍하기 위한 MP4 파일을 지정한다.
 */
public class VideoStream {

    private static final Logger logger = LoggerFactory.getLogger(VideoStream.class);

    private final String mp4FileName;
    private final String resultM3U8FilePath;

    ////////////////////////////////////////////////////////////////////////////////

    public VideoStream(String uri) {
        String mp4FileName = RtspUnit.getPureFileName(uri);

        if (mp4FileName.endsWith(".m3u8")) {
            mp4FileName = mp4FileName.substring(
                    0,
                    mp4FileName.lastIndexOf(".")
            );
            mp4FileName += ".mp4";
        }

        logger.debug("Target mp4FileName={}", mp4FileName);
        this.mp4FileName = mp4FileName;

        String mp4FilePath = mp4FileName.substring(
                0,
                mp4FileName.lastIndexOf("/")
        );

        String mp4FileNameOnly = mp4FileName.substring(
                mp4FileName.lastIndexOf("/") + 1,
                mp4FileName.lastIndexOf(".")
        );

        resultM3U8FilePath = mp4FilePath + File.separator + mp4FileNameOnly + ".m3u8";
        logger.debug("mp4FileName={}, resultM3U8FilePath={}", mp4FileName, resultM3U8FilePath);
    }

    /////////////////////////////////////////////////////////////////////

    public String getMp4FileName() {
        return mp4FileName;
    }

    public String getResultM3U8FilePath() {
        return resultM3U8FilePath;
    }

    /////////////////////////////////////////////////////////////////////


    @Override
    public String toString() {
        return "VideoStream{" +
                "mp4FileName='" + mp4FileName + '\'' +
                ", resultM3U8FilePath='" + resultM3U8FilePath + '\'' +
                '}';
    }
}