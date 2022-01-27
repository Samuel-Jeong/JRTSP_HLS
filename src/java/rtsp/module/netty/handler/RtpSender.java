package rtsp.module.netty.handler;

import com.fsm.module.StateHandler;
import com.google.common.collect.Maps;
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
import rtsp.module.mpegts.content.PATSection;
import rtsp.module.mpegts.content.PMTSection;
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
import java.util.Objects;
import java.util.TreeMap;
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
                    rtspUnit.getRtspUnitId(), streamer.getSessionId(), streamer.getDestIp(), streamer.getDestPort(),
                    new String(m3u8ByteData, StandardCharsets.UTF_8), m3u8ByteData.length
            );
            ///////////////////////////////////////////////////////////////////////////

            ///////////////////////////////////////////////////////////////////////////
            // SEND TS FILES
            mediaSegmentList = streamer.getMediaSegmentList();
            String m3u8PathOnly = streamer.getM3u8PathOnly();

            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

            // TS Packet Total byte : 188 (4(header) + 184(body))
            // > 이 하나의 패킷 안에 하나의 이미지(프레임)에 대한 모든 정보가 들어있는게 아니다.
            byte[] buffer = new byte[TS_PACKET_SIZE];

            int tbn = 0;
            int fps = 0;
            int gop;

            long additionalTimestampIncrement = 1;
            long frameCount = 0;
            long packetCount = 0;
            long totalSleepTime = 0; // ms
            int totalSendByteSize = 0;
            boolean isEndOfFrame;

            List<InputStream> inputStreamList = new ArrayList<>();

            try {
                ///////////////////////////////////////////////////////////////////////////
                // GET TS FILE NAME & STREAM LIST
                for (MediaSegment mediaSegment : mediaSegmentList) {
                    if (mediaSegment == null) {
                        continue;
                    }

                    ///////////////////////////////////////////////////////////////////////////
                    // GET TS FILE NAME & STREAM
                    String tsFileName = mediaSegment.uri();
                    tsFileName = m3u8PathOnly + File.separator + tsFileName;
                    InputStream inputStream = new FileInputStream(tsFileName);

                    if (fps == 0) {
                        fps = Integer.parseInt(Objects.requireNonNull(getFps(tsFileName))); // fps
                        gop = getGop(tsFileName); // gop
                        tbn = Integer.parseInt(Objects.requireNonNull(getTbn(tsFileName)));
                        logger.debug("({}) ({}) FPS=[{}], GOP=[{}], TBN=[{}]",
                                rtspUnit.getRtspUnitId(), streamer.getSessionId(),
                                fps, gop, tbn
                        );
                    }
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
                        logger.debug("({}) ({}) MPEG TS({}) FileSize=[{}]",
                                rtspUnit.getRtspUnitId(), streamer.getSessionId(),
                                tsFileName, fileSize
                        );
                    }

                    inputStreamList.add(inputStream);
                }
                ///////////////////////////////////////////////////////////////////////////

                ///////////////////////////////////////////////////////////////////////////
                // START TO STREAM
                for (InputStream inputStream : inputStreamList) {
                    if (inputStream == null) { continue; }

                    int fileSize = inputStream.available();
                    int read;
                    int curTsTotalByteSize = 0;

                    boolean resetState = false;
                    long pcrCount = 0;
                    Long firstPcrValue = null;
                    //Long firstPcrTime = null;
                    Long lastPcrValue = null;
                    Long lastPcrTime = null;

                    ///////////////////////////////////////////////////////////////////////////
                    // [RTP]
                    while ((read = inputStream.read(buffer)) != -1) {
                        if (streamer.isPaused()) { break; }

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
                        ByteBuffer byteBuffer = ByteBuffer.wrap(curData);
                        MpegTsPacket mpegTsPacket = new MpegTsPacket(byteBuffer);
                        //logger.debug("({}) ({}) MpegTsPacket: \n[{}]", rtspUnit.getRtspUnitId(), streamer.getSessionId(), mpegTsPacket);
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        if (resetState) {
                            pcrCount = 0;
                            firstPcrValue = null;
                            //firstPcrTime = null;
                            lastPcrValue = null;
                            lastPcrTime = null;
                            resetState = false;
                        }

                        long sleepNanos = 0;
                        int pid = mpegTsPacket.getPid();
                        PATSection patSection = null;
                        TreeMap<Integer, PMTSection> pmtSection = Maps.newTreeMap();

                        ///////////////////////////////////////////////////////////////////////////
                        // CHECK PMT
                        if (pid == 0 && mpegTsPacket.isPayloadUnitStartIndicator()) {
                            ByteBuffer payload = mpegTsPacket.getPayload();
                            payload.rewind();
                            int pointer = payload.get() & 0xff;
                            payload.position(payload.position() + pointer);
                            patSection = PATSection.parse(payload);
                            if (patSection != null) {
                                for (Integer pmtPid : pmtSection.keySet()) {
                                    if (!patSection.getPrograms().containsValue(pmtPid)) {
                                        pmtSection.remove(pmtPid);
                                    }
                                }
                            }
                        }
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // CHECK PAT
                        if (pid != 0 && patSection != null) {
                            if (patSection.getPrograms().containsValue(pid)) {
                                if (mpegTsPacket.isPayloadUnitStartIndicator()) {
                                    ByteBuffer payload = mpegTsPacket.getPayload();
                                    payload.rewind();
                                    int pointer = payload.get() & 0xff;
                                    payload.position(payload.position() + pointer);
                                    pmtSection.put(pid, PMTSection.parse(payload));
                                }
                            }
                        }
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // CHECK PCR
                        MpegTsPacket.AdaptationField adaptationField = mpegTsPacket.getAdaptationField();
                        if (adaptationField != null) {
                            if (adaptationField.getPcr() != null) {
                                if (!adaptationField.isDiscontinuityIndicator()) {
                                    // Get PCR and current nano time
                                    long pcrValue = adaptationField.getPcr().getValue();
                                    long pcrTime = System.nanoTime();
                                    pcrCount++;

                                    // Compute sleepNanosOrig
                                    //Long sleepNanosOrig = null;
                                    if (firstPcrValue == null) {
                                        firstPcrValue = pcrValue;
                                        //firstPcrTime = pcrTime;
                                    } /*else if (pcrValue > firstPcrValue) {
                                        // ts-container has fixed time-scale (90kHZ for PTS/DTS and 27MHz for PCR)
                                        sleepNanosOrig = ((pcrValue - firstPcrValue) / 27 * 1000) - (pcrTime - firstPcrTime);
                                    }*/

                                    // Compute sleepNanosPrevious
                                    Long sleepNanosPrevious = null;
                                    if (lastPcrValue != null && lastPcrTime != null) {
                                        if (pcrValue <= lastPcrValue) {
                                            logger.warn("({}) ({}) PCR discontinuity ! (pid={}, pcrValue={}, lastPcrValue={})", rtspUnit.getRtspUnitId(), streamer.getSessionId(), mpegTsPacket.getPid(), pcrValue, lastPcrValue);
                                            resetState = true;
                                        } else {
                                            // ts-container has fixed time-scale (90kHZ for PTS/DTS and 27MHz for PCR)
                                            sleepNanosPrevious = ((pcrValue - lastPcrValue) / 27 * 1000) - (pcrTime - lastPcrTime);
                                        }
                                    }

                                    // Set sleep time based on PCR if possible
                                    if (sleepNanosPrevious != null) {
                                        // Safety : We should never have to wait more than 100ms
                                        if (sleepNanosPrevious > 100000000) {
                                            logger.warn("({}) ({}) PCR sleep ignored, too high! (pid={}, sleepNanosPrevious={})", rtspUnit.getRtspUnitId(), streamer.getSessionId(), mpegTsPacket.getPid(), sleepNanosPrevious);
                                            resetState = true;
                                        } else {
                                            sleepNanos = sleepNanosPrevious;
                                        }
                                    }

                                    // Set lastPcrValue/lastPcrTime
                                    lastPcrValue = pcrValue;
                                    lastPcrTime = pcrTime + sleepNanos;
                                } else {
                                    logger.warn("({}) ({}) Skipped PCR - Discontinuity indicator", rtspUnit.getRtspUnitId(), streamer.getSessionId());
                                }
                            }
                        }
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // Sleep if needed
                        if (sleepNanos > 0) {
                            try {
                                Thread.sleep(sleepNanos / 1000000, (int) (sleepNanos % 1000000));
                            } catch (InterruptedException e) {
                                logger.warn("({}) ({}) Streaming sleep interrupted!", rtspUnit.getRtspUnitId(), streamer.getSessionId());
                            }

                            if (adaptationField.isRandomAccessIndicator()) { // KEY FRAME
                                additionalTimestampIncrement = (frameCount + 1); // I-FRAME INTERVAL FOR TIMESTAMP
                                frameCount = 0;
                            } else {
                                additionalTimestampIncrement = 1;
                                frameCount++;
                            }
                            logger.debug("frameCount: {}, additionalTimestampIncrement: {}", frameCount, additionalTimestampIncrement);

                            isEndOfFrame = true;
                        } else {
                            isEndOfFrame = false;
                        }
                        ///////////////////////////////////////////////////////////////////////////

                        ///////////////////////////////////////////////////////////////////////////
                        // SEND RTP PACKET
                        sendRtpPacket(streamer, curData, fps, tbn, isEndOfFrame, additionalTimestampIncrement);
                        curTsTotalByteSize += curData.length; // TS 파일 누적 크기 계산 (Ts 파일 구분)
                        packetCount++;
                        ///////////////////////////////////////////////////////////////////////////
                    }
                    ///////////////////////////////////////////////////////////////////////////

                    ///////////////////////////////////////////////////////////////////////////
                    // FINISH
                    totalSendByteSize += curTsTotalByteSize;
                    logger.debug("({}) ({}) [SEND TS BYTES: {}({}), [PCR: {},  PACKET: {}]",
                            rtspUnit.getRtspUnitId(), streamer.getSessionId(),
                            curTsTotalByteSize, fileSize, pcrCount, packetCount
                    );
                    inputStream.close();
                    if (streamer.isPaused()) {
                        logger.warn("({}) ({}) [FINISHED BY PAUSE]", rtspUnit.getRtspUnitId(), streamer.getSessionId());
                        break;
                    }
                    ///////////////////////////////////////////////////////////////////////////
                }
            } finally {
                logger.debug("({}) ({}) [SEND TOTAL BYTES: {}, PACKET COUNT: {}, SLEEP TIME: {}]", rtspUnit.getRtspUnitId(), streamer.getSessionId(), totalSendByteSize, packetCount, totalSleepTime);

                try { byteArrayOutputStream.close(); } catch (IOException e) { logger.warn("", e); }
                try {
                    for (InputStream inputStream : inputStreamList) {
                        if (inputStream != null) {
                            inputStream.close();
                        }
                    }
                } catch (IOException e) { logger.warn("", e); }
            }
            ///////////////////////////////////////////////////////////////////////////
        } catch (Exception e) {
            logger.warn("RtspChannelHandler.sendData.Exception", e);
        }
    }

    private void sendRtpPacket(Streamer streamer, byte[] data, int fps, int tbn, boolean isEndOfFrame, long additionalTimestampIncrement) {
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

        if (curSeqNum == 65535) {
            streamer.resetSeqNumber();
            curSeqNum = streamer.getCurSeqNum();
        }
        streamer.setCurSeqNum(curSeqNum + 1);

        /**
         * For video, time clock rate is fixed at 90 kHz.
         * The timestamps generated depend on whether the application can determine the frame number or not.
         * If it can or it can be sure that it is transmitting every frame with a fixed frame rate,
         *      the timestamp is governed by the nominal frame rate.
         * Thus, for a 30 f/s video,
         *      timestamps would increase by 3,000 for each frame,
         *      for a 25 f/s video by 3,600 for each frame.
         * If a frame is transmitted as several RTP packets,
         *      these packets would all bear the same timestamp.
         *  If the frame number cannot be determined or if frames are sampled aperiodically,
         *      as is typically the case for software codecs,
         *      the timestamp has to be computed from the system clock (e.g., gettimeofday()).
         */
        if (isEndOfFrame) {
            streamer.setCurTimeStamp(curTimeStamp + ((tbn / fps) * additionalTimestampIncrement));
        }
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

    private String getFrameStartTime(String tsFileName) {
        String frameStartTimeString = ffmpegManager.getFrameStartTime(tsFileName);
        if (frameStartTimeString != null) {
            String[] splitLine = frameStartTimeString.split(",");
            return splitLine[1];
        }

        return null;
    }

    private String getFps(String tsFileName) {
        String fpsString = ffmpegManager.getFps(tsFileName);
        if (fpsString != null) {
            String[] splitLine = fpsString.split(",");
            return splitLine[2].split("/")[0]; // 30/1 > 30
        }

        return null;
    }

    private int getGop(String tsFileName) {
        int lineNumber = 0;
        int gop = 0; // 0 < gop <= 30

        // I, B, P, B, B, I, ...
        // > gop = 5

        // I, I, I, ...
        // > gop = 1

        List<String> frameLineList = ffmpegManager.getFrameLineList(tsFileName);
        if (!frameLineList.isEmpty()) {
            for (String frameLine : frameLineList) { // ex) frame,20464,I
                if (frameLine != null && frameLine.length() > 0) {
                    String[] splitLine = frameLine.split(",");

                    // I-FRAME 간격 구하기
                    if (lineNumber > 0 && splitLine[2].equals("I")) {
                        break;
                    } else {
                        gop++;
                    }
                }

                lineNumber++;
            }
        }
        return gop;
    }

    private String getTbn(String tsFileName) {
        String tbnString = ffmpegManager.getTbn(tsFileName);
        if (tbnString != null) {
            String[] splitLine = tbnString.split(",");
            return splitLine[2].split("/")[1]; // 1/90000 > 90000
        }

        return null;
    }

}
