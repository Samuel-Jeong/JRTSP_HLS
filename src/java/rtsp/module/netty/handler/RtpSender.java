package rtsp.module.netty.handler;

import com.fsm.module.StateHandler;
import io.lindstrom.m3u8.model.MediaPlaylist;
import io.lindstrom.m3u8.model.MediaSegment;
import io.lindstrom.m3u8.parser.MediaPlaylistParser;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.config.ConfigManager;
import rtsp.ffmpeg.FfmpegManager;
import rtsp.fsm.RtspEvent;
import rtsp.module.Streamer;
import rtsp.module.VideoStream;
import rtsp.module.base.RtspUnit;
import rtsp.module.mpegts.content.MpegTsPacket;
import rtsp.protocol.RtpPacket;
import rtsp.service.AppInstance;
import rtsp.service.scheduler.job.Job;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RtpSender extends Job {
    
    private static final Logger logger = LoggerFactory.getLogger(RtpSender.class);

    ///////////////////////////////////////////////////////////////////////////
    public static final int TS_PACKET_SIZE = 188;

    private final RtpPacket rtpPacket = new RtpPacket();

    private final FfmpegManager ffmpegManager;
    private final VideoStream video;
    private final double fileTime;
    private final double npt1;
    private final double npt2;
    private final StateHandler rtspStateHandler;
    private final RtspUnit rtspUnit;
    private final Streamer streamer;
    private final int destPort;
    ///////////////////////////////////////////////////////////////////////////

    ///////////////////////////////////////////////////////////////////////////

    /**
     * @param name Job 이름
     * @param initialDelay Job 최초 실행 전 딜레이
     * @param interval Job 실행 간격 시간
     * @param timeUnit Job 실행 간격 시간 단위
     * @param priority Job 우선순위
     * @param totalRunCount Job 전체 실행 횟수
     * @param isLasted Job 영구 진행 여부
     * @param ffmpegManager FfmpegManager
     * @param video VideoStream
     * @param fileTime HLS interval time
     * @param npt1 Start time
     * @param npt2 End time
     * @param rtspStateHandler StateHandler
     * @param rtspUnit RtspUnit
     * @param streamer Streamer
     * @param destPort Destination RTP Port
     */
    public RtpSender(String name,
                     int initialDelay, int interval, TimeUnit timeUnit,
                     int priority, int totalRunCount, boolean isLasted,
                     FfmpegManager ffmpegManager, VideoStream video,
                     double fileTime, double npt1, double npt2,
                     StateHandler rtspStateHandler, RtspUnit rtspUnit, Streamer streamer, int destPort) {
        super(name, initialDelay, interval, timeUnit, priority, totalRunCount, isLasted);

        this.ffmpegManager = ffmpegManager;
        this.video = video;
        this.fileTime = fileTime;
        this.npt1 = npt1;
        this.npt2 = npt2;
        this.rtspStateHandler = rtspStateHandler;
        this.rtspUnit = rtspUnit;
        this.streamer = streamer;
        this.destPort = destPort;
    }
    ///////////////////////////////////////////////////////////////////////////

    @Override
    public void run() {
        sendData();
    }

    /**
     * @fn private void sendData()
     * @brief 미리 생성된 M3U8 파일에 명시된 TS 파일을 로컬에서 읽어서 지정한 Destination 으로 RTP 패킷으로 패킹하여 보내는 함수
     */
    private void sendData() {
        try {
            ///////////////////////////////////////////////////////////////////////////
            // DIRECT PARSING IF ENABLED
            ConfigManager configManager = AppInstance.getInstance().getConfigManager();
            if (configManager.isM3u8DirectConverting()) {
                ffmpegManager.convertMp4ToM3u8(
                        video.getMp4FileName(),
                        video.getResultM3U8FilePath(),
                        (long) fileTime,
                        (long) npt1,
                        (long) npt2
                );
            }
            ///////////////////////////////////////////////////////////////////////////

            ///////////////////////////////////////////////////////////////////////////
            // CHECK M3U8 FILE
            File m3u8File = new File(video.getResultM3U8FilePath());
            if (!m3u8File.exists() || !m3u8File.isFile()) {
                logger.warn("({}) ({}) M3U8 File is wrong.Fail to get the m3u8 data. (m3u8FilePath={})", rtspUnit.getRtspUnitId(), streamer.getSessionId(), video.getResultM3U8FilePath());
                return;
            }

            byte[] m3u8ByteData = Files.readAllBytes(
                    Paths.get(
                            video.getResultM3U8FilePath()
                    )
            );

            if (m3u8ByteData.length == 0) {
                logger.warn("({}) ({}) Fail to process the PLAY request. Fail to get the m3u8 data. (rtspUnit={}, destPort={})", rtspUnit.getRtspUnitId(), streamer.getSessionId(), rtspUnit, destPort);
                rtspStateHandler.fire(
                        RtspEvent.PLAY_FAIL,
                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                );
                return;
            }
            ///////////////////////////////////////////////////////////////////////////

            ///////////////////////////////////////////////////////////////////////////
            // GET MEDIA SEGMENT LIST
            List<MediaSegment> mediaSegmentList;
            MediaPlaylistParser parser = new MediaPlaylistParser();
            MediaPlaylist playlist = parser.readPlaylist(Paths.get(video.getResultM3U8FilePath()));
            if (playlist != null) {
                String m3u8PathOnly = video.getResultM3U8FilePath();
                m3u8PathOnly = m3u8PathOnly.substring(
                        0,
                        m3u8PathOnly.lastIndexOf("/")
                );
                mediaSegmentList = playlist.mediaSegments();
                streamer.setM3u8PathOnly(m3u8PathOnly);
                streamer.setMediaSegmentList(mediaSegmentList);

                logger.debug("({}) ({}) MediaPlaylist: {}", rtspUnit.getRtspUnitId(), streamer.getSessionId(), playlist);
            } else {
                logger.warn("({}) ({}) Fail to stream the media. (rtpDestPort={})", rtspUnit.getRtspUnitId(), streamer.getSessionId(), destPort);
                rtspStateHandler.fire(
                        RtspEvent.PLAY_FAIL,
                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                );
                return;
            }

            if (mediaSegmentList == null || mediaSegmentList.isEmpty()) {
                logger.warn("({}) ({}) Media segment list is empty.", rtspUnit.getRtspUnitId(), streamer.getSessionId());
                rtspStateHandler.fire(
                        RtspEvent.PLAY_FAIL,
                        rtspUnit.getStateManager().getStateUnit(rtspUnit.getRtspStateUnitId())
                );
                return;
            }
            ///////////////////////////////////////////////////////////////////////////

            ///////////////////////////////////////////////////////////////////////////
            // SEND M3U8
            ByteBuf buf = Unpooled.copiedBuffer(m3u8ByteData);
            streamer.send(
                    buf,
                    streamer.getDestIp(),
                    streamer.getDestPort()
            );

            logger.debug("({}) ({}) << Send M3U8 (destIp={}, destPort={})\n{}(size={})",
                    getName(), rtspUnit.getRtspUnitId(),
                    streamer.getSessionId(), streamer.getDestIp(), streamer.getDestPort(),
                    new String(m3u8ByteData, StandardCharsets.UTF_8), m3u8ByteData.length
            );
            ///////////////////////////////////////////////////////////////////////////

            ///////////////////////////////////////////////////////////////////////////
            // SEND TS FILES
            mediaSegmentList = streamer.getMediaSegmentList();
            String m3u8PathOnly = streamer.getM3u8PathOnly();

            int fps = configManager.getFps();
            int gop = configManager.getGop();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            InputStream inputStream = null;

            // TS Packet Total byte : 188 (4(header) + 184(body))
            // > 이 하나의 패킷 안에 하나의 이미지(프레임)에 대한 모든 정보가 들어있는게 아니다.
            byte[] buffer = new byte[TS_PACKET_SIZE];

            int timeInterval = (fps * gop); // i-frame interval (ms)
            long timeStampInterval = 90000 / timeInterval;
            logger.debug("({}) ({}) RtpInterval=[{}], TimeStampInterval=[{}]",
                    rtspUnit.getRtspUnitId(), streamer.getSessionId(), timeInterval, timeStampInterval
            );

            //int curTimeInterval;
            int read;
            int totalSendByteSize = 0;
            int totalFrameCount = 0;
            long curTotalFrameSize = 0;
            TimeUnit timeUnit = TimeUnit.MILLISECONDS;

            try {
                for (MediaSegment mediaSegment : mediaSegmentList) {
                    if (mediaSegment == null) { continue; }

                    ///////////////////////////////////////////////////////////////////////////
                    // GET TS FILE NAME & STREAM
                    String tsFileName = mediaSegment.uri();
                    tsFileName = m3u8PathOnly + File.separator + tsFileName;
                    inputStream = new FileInputStream(tsFileName);
                    ///////////////////////////////////////////////////////////////////////////

                    ///////////////////////////////////////////////////////////////////////////
                    // CHECK FILE SIZE
                    int fileSize = inputStream.available();
                    if (fileSize <= 0) {
                        logger.warn("({}) ({}) Fail to read the ts file. FileSize=[{}]",
                                rtspUnit.getRtspUnitId(), streamer.getSessionId(), fileSize
                        );
                        continue;
                    } else {
                        logger.warn("({}) ({}) MPEG TS({}) FileSize=[{}]",
                                rtspUnit.getRtspUnitId(), streamer.getSessionId(),
                                tsFileName, fileSize
                        );
                    }

                    ///////////////////////////////////////////////////////////////////////////
                    // GET FRAME SIZE LIST FROM THE CURRENT TS FILE
                    List<String[]> frameSizeList = getTsFileFrameSizeList(tsFileName);
                    ///////////////////////////////////////////////////////////////////////////

                    ///////////////////////////////////////////////////////////////////////////
                    // [RTP]
                    int curTsTotalByteSize = 0;
                    int curFrameCount = 0;
                    long frameRemainSizeInc = 0; // 누적 잉여 프레임 바이트 사이즈
                    long curTimeStampInterval;
                    boolean isSleep;

                    while ((read = inputStream.read(buffer)) != -1) {
                        if (streamer.isPaused()) { break; }
                        if (curFrameCount >= frameSizeList.size()) { break; }

                        ///////////////////////////////////////////////////////////////////////////
                        // [RTCP]
                        //curTimeInterval = timeInterval * (rtspUnit.getCongestionLevel() + 1);
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // GET DATA (188 bytes, static)
                        byteArrayOutputStream.reset();
                        byteArrayOutputStream.write(buffer, 0, read);
                        byte[] curData = byteArrayOutputStream.toByteArray();
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // TS PACKET DECODING (PAT, PMT, PSI)
                        /*ByteBuffer byteBuffer = ByteBuffer.wrap(curData);
                        MpegTsPacket mpegTsPacket = new MpegTsPacket(byteBuffer);
                        logger.debug("({}) ({}) MpegTsPacket: \n[{}]", rtspUnit.getRtspUnitId(), streamer.getSessionId(), mpegTsPacket);*/
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // GET FRAME DATA (size, type)
                        String[] curFrameInfo = frameSizeList.get(curFrameCount);
                        long curFrameSize = Long.parseLong(curFrameInfo[0]);
                        String curFrameType = curFrameInfo[1];
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // NEXT FRAME
                        if (curTotalFrameSize >= (curFrameSize + frameRemainSizeInc)) {
                            curTimeStampInterval = timeStampInterval;
                            curTotalFrameSize -= curFrameSize;
                            frameRemainSizeInc = curTotalFrameSize;
                            curFrameCount++;
                            isSleep = true;
                        } else { // CURRENT FRAME
                            curTimeStampInterval = 0;
                            isSleep = false;
                        }
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // SEND RTP PACKET
                        sendRtpPacket(streamer, curData, curTimeStampInterval);
                        if (!isSleep) {
                            curTotalFrameSize += curData.length; // 프레임 크기 누적 계산 (프레임 구분)
                            curTsTotalByteSize += curData.length; // TS 파일 누적 크기 계산 (Ts 파일 구분)
                        }
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // SLEEP IF CURRENT FRAME IS SENT
                        if (curTsTotalByteSize < fileSize) { // 파일의 마지막에는 SLEEP 하지 않음
                            if (isSleep) {
                                timeUnit.sleep(timeInterval);
                                logger.debug("({}) ({}) [SLEEP({}ms)] [curTsTotalByteSize={}, fileSize={}], [curFrameCount={}, curFrameType={}, curFrameSize={}(+{}), curTotalFrameSize(remain)={}]",
                                        rtspUnit.getRtspUnitId(), streamer.getSessionId(),
                                        timeInterval,
                                        curTsTotalByteSize, fileSize,
                                        curFrameCount, curFrameType, curFrameSize, frameRemainSizeInc, curTotalFrameSize
                                );
                            }
                        } else {
                            logger.warn("END OF FILE > NOT SLEEP");
                        }
                        ///////////////////////////////////////////////////////////////////////////
                    }

                    // [UDP] Set up packet source
                    /*MTSSource movie = MTSSources.from(new File(tsFileName));
                    MTSSink transport = UDPTransport.builder()
                            .setAddress(streamer.getDestIp()) // Can be a multicast address
                            .setPort(streamer.getDestPort())
                            .setSoTimeout(1000)
                            .setTtl(1)
                            .build();
                    MpegTsStreamer mpegTsStreamer = MpegTsStreamer.builder()
                            .setSource(movie)
                            .setSink(transport)
                            .build();
                    mpegTsStreamer.stream();*/
                    ///////////////////////////////////////////////////////////////////////////

                    totalFrameCount += curFrameCount;
                    totalSendByteSize += curTsTotalByteSize;
                    logger.debug("({}) ({}) [SEND TS BYTES: {}, FRAME COUNT: {}] (bitrate={}, {})", rtspUnit.getRtspUnitId(), streamer.getSessionId(), curTsTotalByteSize, curFrameCount, mediaSegment.bitrate(), mediaSegment);

                    inputStream.close();
                    inputStream = null;

                    if (streamer.isPaused()) {
                        logger.warn("({}) ({}) [FINISHED BY PAUSE]", rtspUnit.getRtspUnitId(), streamer.getSessionId());
                        break;
                    }

                    //logger.debug("({}) ({}) [SEND TS] (bitrate={}, {})", rtspUnit.getRtspUnitId(), streamer.getSessionId(), mediaSegment.bitrate(), mediaSegment);
                    /*long sec = (long) mediaSegment.duration();
                    long msec = (long) ((mediaSegment.duration() - sec) * 1000);
                    long timeout = sec * 1000 + msec;
                    logger.debug("({}) ({}) SLEEP: {}", rtspUnit.getRtspUnitId(), streamer.getSessionId(), timeout);
                    timeUnit.sleep(timeout);*/
                }
            } finally {
                logger.debug("({}) ({}) [SEND TOTAL BYTES: {}, FRAME COUNT: {}]", rtspUnit.getRtspUnitId(), streamer.getSessionId(), totalSendByteSize, totalFrameCount);

                try { byteArrayOutputStream.close(); } catch (IOException e) { logger.warn("", e); }
                try { if (inputStream != null) { inputStream.close(); } } catch (IOException e) { logger.warn("", e); }
            }
            ///////////////////////////////////////////////////////////////////////////
        } catch (Exception e) {
            logger.warn("RtspChannelHandler.sendData.Exception", e);
        }
    }

    private void sendRtpPacket(Streamer streamer, byte[] data, long timeStampInterval) {
        int curSeqNum = streamer.getCurSeqNum();
        long curTimeStamp = streamer.getCurTimeStamp();

        rtpPacket.setValue(
                2, 0, 0, 0, 0, ConfigManager.MP2T_TYPE,
                curSeqNum, curTimeStamp, streamer.getSsrc(), data, data.length
        );

        byte[] totalRtpData = rtpPacket.getData();
        ByteBuf buf = Unpooled.copiedBuffer(totalRtpData);
        streamer.send(
                buf,
                streamer.getDestIp(),
                streamer.getDestPort()
        );

        streamer.setCurSeqNum(curSeqNum + 1);
        streamer.setCurTimeStamp(curTimeStamp + timeStampInterval);
    }

    private List<String[]> getTsFileFrameSizeList(String tsFileName) {
        List<String[]> frameSizeList = new ArrayList<>();
        List<String> frameLineList = ffmpegManager.getFrameLineList(tsFileName);
        if (!frameLineList.isEmpty()) {
            for (String frameLine : frameLineList) { // ex) frame,20464,I
                if (frameLine != null && frameLine.length() > 0) {
                    String[] splitLine = frameLine.split(",");

                    // I, P, B FRAME 모두 추가
                    frameSizeList.add(new String[]{splitLine[1], splitLine[2]});
                }
            }
        }
        return frameSizeList;
    }

}
