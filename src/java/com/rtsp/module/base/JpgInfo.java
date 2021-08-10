package com.rtsp.module.base;

import java.awt.image.BufferedImage;

/**
 * @class public class JpgInfo
 * @brief JpgInfo class
 */
public class JpgInfo {

    private final BufferedImage bufferedImage;
    private final String tempJpgFilePath;
    private final String fileNameOnly;
    private final String resultJpgFilePath;
    private final String resultTsFilePath;
    private final double curTime;
    private final double totalTime;
    private final long curFrameCount;

    public JpgInfo(BufferedImage bufferedImage, String tempJpgFilePath, String fileNameOnly, String resultJpgFilePath, String resultTsFilePath, double curTime, double totalTime, long curFrameCount) {
        this.bufferedImage = bufferedImage;
        this.tempJpgFilePath = tempJpgFilePath;
        this.fileNameOnly = fileNameOnly;
        this.resultJpgFilePath = resultJpgFilePath;
        this.resultTsFilePath = resultTsFilePath;
        this.curTime = curTime;
        this.totalTime = totalTime;
        this.curFrameCount = curFrameCount;
    }

    public BufferedImage getBufferedImage() {
        return bufferedImage;
    }

    public String getTempJpgFilePath() {
        return tempJpgFilePath;
    }

    public String getFileNameOnly() {
        return fileNameOnly;
    }

    public String getResultJpgFilePath() {
        return resultJpgFilePath;
    }

    public String getResultTsFilePath() {
        return resultTsFilePath;
    }

    public double getCurTime() {
        return curTime;
    }

    public double getTotalTime() {
        return totalTime;
    }

    public long getCurFrameCount() {
        return curFrameCount;
    }
}
