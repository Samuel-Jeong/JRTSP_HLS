package com.rtsp.module.base;

import com.rtsp.fsm.RtspState;
import com.rtsp.module.netty.NettyChannelManager;
import com.rtsp.module.netty.base.NettyChannelType;
import com.rtsp.module.netty.module.NettyChannel;
import io.lindstrom.m3u8.model.MediaSegment;

import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @class public class RtspUnit
 * @brief RtspUnit class
 */
public class RtspUnit {

    private String rtspId = UUID.randomUUID().toString(); // ID of the RTSP session

    private final String rtspStateUnitId;

    private String uri;
    private String fileName;

    private int congestionLevel = 0;

    private NettyChannel rtspChannel;
    private NettyChannel rtcpChannel;

    private final AtomicBoolean isInterleaved = new AtomicBoolean(false);

    private String sessionId = null;
    private int ssrc;

    private String destIp;
    private int destPort = -1; // rtp destination port

    private static final int rtcpListenPort = 19001; // rtcp listen port
    private int rtcpDestPort = -1; // rtcp destination port

    private final Random random = new Random();

    private int curSeqNum;
    private int curTimeStamp;

    private boolean isMediaEnabled = false;
    private List<MediaSegment> mediaSegmentList = null;
    private String m3u8PathOnly = null;

    ////////////////////////////////////////////////////////////////////////////////

    public RtspUnit(String listenIp, int listenPort) {
        rtspChannel = NettyChannelManager.getInstance().openChannel(listenIp, listenPort, NettyChannelType.RTSP);
        rtcpChannel = NettyChannelManager.getInstance().openChannel(listenIp, rtcpListenPort, NettyChannelType.RTCP);

        ssrc = random.nextInt(Integer.MAX_VALUE);

        this.rtspStateUnitId = String.valueOf(UUID.randomUUID());

        curSeqNum = random.nextInt(65536);
        curTimeStamp = random.nextInt(65536);
    }

    ////////////////////////////////////////////////////////////////////////////////
    
    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public int getCurSeqNum() {
        return curSeqNum;
    }

    public void setCurSeqNum(int curSeqNum) {
        this.curSeqNum = curSeqNum;
    }

    public int getCurTimeStamp() {
        return curTimeStamp;
    }

    public void setCurTimeStamp(int curTimeStamp) {
        this.curTimeStamp = curTimeStamp;
    }

    public boolean isMediaEnabled() {
        return isMediaEnabled;
    }

    public void setMediaEnabled(boolean mediaEnabled) {
        isMediaEnabled = mediaEnabled;
    }

    public List<MediaSegment> getMediaSegmentList() {
        return mediaSegmentList;
    }

    public void setMediaSegmentList(List<MediaSegment> mediaSegmentList) {
        this.mediaSegmentList = mediaSegmentList;
    }

    public String getM3u8PathOnly() {
        return m3u8PathOnly;
    }

    public void setM3u8PathOnly(String m3u8PathOnly) {
        this.m3u8PathOnly = m3u8PathOnly;
    }

    public String getRtspStateUnitId() {
        return rtspStateUnitId;
    }

    public int getSsrc() {
        return ssrc;
    }

    public void setSsrc() {
        this.ssrc = random.nextInt(Integer.MAX_VALUE);
    }

    public String getRtspId() {
        return rtspId;
    }

    public void setRtspId(String rtspId) {
        this.rtspId = rtspId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getRtcpDestPort() {
        return rtcpDestPort;
    }

    public void setRtcpDestPort(int rtcpDestPort) {
        this.rtcpDestPort = rtcpDestPort;
    }

    public int getCongestionLevel() {
        return congestionLevel;
    }

    public void setCongestionLevel(int congestionLevel) {
        this.congestionLevel = congestionLevel;
    }

    public NettyChannel getRtspChannel() {
        return rtspChannel;
    }

    public void setRtspChannel(NettyChannel rtspChannel) {
        this.rtspChannel = rtspChannel;
    }

    public NettyChannel getRtcpChannel() {
        return rtcpChannel;
    }

    public void setRtcpChannel(NettyChannel rtcpChannel) {
        this.rtcpChannel = rtcpChannel;
    }

    public boolean isInterleaved() {
        return isInterleaved.get();
    }

    public void setInterleaved(boolean interleaved) {
        isInterleaved.set(interleaved);
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getDestIp() {
        return destIp;
    }

    public void setDestIp(String destIp) {
        this.destIp = destIp;
    }

    public int getDestPort() {
        return destPort;
    }

    public void setDestPort(int destPort) {
        this.destPort = destPort;
    }

    public static String getPureFileName(String uri) {
        if (uri.startsWith("rtsp")) {
            String curFileName = uri;
            String rtspStr = curFileName.substring(
                    curFileName.lastIndexOf("rtsp:") + 8
            );

            curFileName = rtspStr.substring(
                    rtspStr.indexOf("/")
            );

            if (curFileName.charAt(curFileName.length() - 1) == '/') {
                curFileName = curFileName.substring(
                        0,
                        curFileName.length() - 1
                );
            }

            return curFileName;
        }

        return uri;
    }

}
