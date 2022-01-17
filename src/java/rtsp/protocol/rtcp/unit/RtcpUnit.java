package rtsp.protocol.rtcp.unit;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.net.ntp.TimeStamp;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.protocol.RtpPacket;
import rtsp.protocol.rtcp.module.Clock;
import rtsp.protocol.rtcp.module.NtpUtils;
import rtsp.protocol.rtcp.module.RtpClock;
import rtsp.protocol.rtcp.type.regular.RtcpSenderReport;

/**
 * @author Henrique Rosa (henrique.rosa@telestax.com)
 * > Redistributed by jamesj to RtcpUnit (before RtcpMember)
 */
public class RtcpUnit {

    private static final Logger logger = LoggerFactory.getLogger(RtcpUnit.class);

    ////////////////////////////////////////////////////////////
    // VARIABLES
    // RTP SEQUENCE NUMBER MOD NUMBER (MAX(LIMIT) NUMBER)
    public static final int RTP_SEQ_MOD = 65536;

    // 현재 패킷보다 다음 패킷의 SEQ_NUM 가 MAX_DROPOUT 값 이상 차이나면 DROP
    public static final int MAX_DROPOUT = 100;

    // 패킷 순서 오류로 판단 가능한 THRESHOLD 최대값
    // - RTP_SEQ_MOD 값에서 MAX_MISORDER 값을 뺀 값이 패킷 순서 오류로 판단 가능한 SEQ_NUM 범위 최대값
    // - SEQ_NUM 가 이 범위 안에 있으면, badSequence 값 지정
    public static final int MAX_MISORDER = 100;

    // RTP SEQ_NUM THRESHOLD 최소값
    // - 2개 이상의 RTP Packet 수신 시점부터 통계 계산
    public static final int MIN_SEQUENTIAL = 2;

    // CLOCKS
    private final RtpClock rtpClock;
    private final Clock wallClock;

    // IDENTIFIERS
    // > 1 : 1 = RtcpUnit : SSRC(or CNAME)
    private long ssrc;
    private String cname;

    // PACKET STATISTICS
    private long receivedPackets;
    private long receivedOctets;
    private long receivedSinceSR;
    private int roundTripDelay;
    private long lastPacketOnReceivedTime;
    private int firstSequenceNumber;
    private int highestSequence;
    private int sequenceCycle;
    private int badSequence;
    private int probation;
    private long receivedPrior;
    private long expectedPrior;

    // JITTER
    // - Measures the relative time it takes for an RTP packet to arrive from the remote server to MMS.
    // - Used to calculate network jitter.
    private long currentTransitDelayTime;
    private long jitter;

    // SenderReport
    private long lastSrTimestamp;
    private long delaySinceLastSrTimestamp; // New
    private long lastSrReceivedOn;
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpUnit(RtpClock clock, long ssrc, String cname) {
        this.rtpClock = clock;
        this.wallClock = clock.getWallClock();
        this.ssrc = ssrc;
        this.cname = cname;
        this.receivedPackets = 0;
        this.receivedOctets = 0;
        this.receivedSinceSR = 0;
        this.lastPacketOnReceivedTime = -1;
        this.firstSequenceNumber = -1;
        this.highestSequence = 0;
        this.badSequence = 0;
        this.sequenceCycle = 0;
        this.probation = 0;
        this.receivedPrior = 0;
        this.expectedPrior = 0;
        this.currentTransitDelayTime = 0;
        this.jitter = -1;
        this.lastSrTimestamp = 0;
        this.delaySinceLastSrTimestamp = 0; // New
        this.lastSrReceivedOn = 0;
        this.roundTripDelay = 0;
    }

    public RtcpUnit(RtpClock clock, long ssrc) {
        this(clock, ssrc, "");
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    public long getSsrc() {
        return ssrc;
    }

    public void setSsrc(long ssrc) {
        this.ssrc = ssrc;
    }

    public String getCname() {
        return cname;
    }

    public void setCname(String cname) {
        this.cname = cname;
    }

    public long getReceivedPackets() {
        return receivedPackets;
    }

    public void setReceivedPackets(long receivedPackets) {
        this.receivedPackets = receivedPackets;
    }

    public long getReceivedOctets() {
        return receivedOctets;
    }

    public void setReceivedOctets(long receivedOctets) {
        this.receivedOctets = receivedOctets;
    }

    public long getReceivedSinceSR() {
        return receivedSinceSR;
    }

    public void setReceivedSinceSR(long receivedSinceSR) {
        this.receivedSinceSR = receivedSinceSR;
    }

    public int getRoundTripDelay() {
        return roundTripDelay;
    }

    public void setRoundTripDelay(int roundTripDelay) {
        this.roundTripDelay = roundTripDelay;
    }

    public long getLastPacketOnReceivedTime() {
        return lastPacketOnReceivedTime;
    }

    public void setLastPacketOnReceivedTime(long lastPacketOnReceivedTime) {
        this.lastPacketOnReceivedTime = lastPacketOnReceivedTime;
    }

    public int getFirstSequenceNumber() {
        return firstSequenceNumber;
    }

    public void setFirstSequenceNumber(int firstSequenceNumber) {
        this.firstSequenceNumber = firstSequenceNumber;
    }

    public int getHighestSequence() {
        return highestSequence;
    }

    public void setHighestSequence(int highestSequence) {
        this.highestSequence = highestSequence;
    }

    public void setSequenceCycle(int sequenceCycle) {
        this.sequenceCycle = sequenceCycle;
    }

    public int getBadSequence() {
        return badSequence;
    }

    public void setBadSequence(int badSequence) {
        this.badSequence = badSequence;
    }

    public int getProbation() {
        return probation;
    }

    public void setProbation(int probation) {
        this.probation = probation;
    }

    public long getReceivedPrior() {
        return receivedPrior;
    }

    public void setReceivedPrior(long receivedPrior) {
        this.receivedPrior = receivedPrior;
    }

    public long getExpectedPrior() {
        return expectedPrior;
    }

    public void setExpectedPrior(long expectedPrior) {
        this.expectedPrior = expectedPrior;
    }

    public long getCurrentTransitDelayTime() {
        return currentTransitDelayTime;
    }

    public void setCurrentTransitDelayTime(long currentTransitDelayTime) {
        this.currentTransitDelayTime = currentTransitDelayTime;
    }

    public long getJitter() {
        return this.jitter >> 4;
    }

    public void setJitter(long jitter) {
        this.jitter = jitter;
    }

    public long getLastSrTimestamp() {
        return lastSrTimestamp;
    }

    public void setLastSrTimestamp(long lastSrTimestamp) {
        this.lastSrTimestamp = lastSrTimestamp;
    }

    public long getLastSrReceivedOn() {
        return lastSrReceivedOn;
    }

    public void setLastSrReceivedOn(long lastSrReceivedOn) {
        this.lastSrReceivedOn = lastSrReceivedOn;
    }

    public int getExtHighSequence() {
        return this.highestSequence + this.sequenceCycle;
    }

    public long getPacketsExpected() {
        return getExtHighSequence() - this.firstSequenceNumber + 1;
    }

    // New
    public long getCumulativeNumberOfPacketsLost() {
        return getPacketsExpected() - this.receivedPackets;
    }

    public long getFractionLost() {
        long expected = getPacketsExpected();
        long expectedInterval = expected - this.expectedPrior;
        this.expectedPrior = expected;

        long receivedInterval = this.receivedPackets - this.receivedPrior;
        this.receivedPrior = this.receivedPackets;

        long lostInterval = expectedInterval - receivedInterval;
        if (expectedInterval == 0 || lostInterval <= 0) {
            return 0;
        }
        return (lostInterval << 8) / expectedInterval;
    }

    public int getSequenceCycle() {
        return (sequenceCycle >> 16);
    }

    public long getLastSRdelay() {
        return getLastSRdelay(this.wallClock.getCurrentTime(), this.lastSrReceivedOn);
    }

    private long getLastSRdelay(long arrivalTime, long lastSrTime) {
        if (this.lastSrReceivedOn == 0) {
            return 0;
        }

        long delay = arrivalTime - lastSrTime;
        // convert to units 1/65536 seconds
        return (long) (delay * 65.536);
    }

    public int getRTT() {
        return Math.max(this.roundTripDelay, 0);
    }

    private void estimateJitter(RtpPacket packet) {
        long transitDelayTime = rtpClock.getLocalRtpTime() - packet.getTimeStamp();
        long d = transitDelayTime - this.currentTransitDelayTime;
        this.currentTransitDelayTime = transitDelayTime;

        if (d < 0) {
            d = -d;
        }

        this.jitter += d - ((this.jitter + 8) >> 4);

        /*logger.debug("[estimateJitter] transitDelay: {}, rtpClock.getLocalRtpTime(): {}, packet.getTimeStamp(): {}", transitDelay, rtpClock.getLocalRtpTime(), packet.getTimeStamp());
        logger.debug("[estimateJitter] d: {}, transitDelay: {}, this.currentTransitDelay: {}", d, transitDelay, this.currentTransitDelay);
        logger.debug("[estimateJitter] currentTransitDelay: {}, jitter: {}", currentTransitDelay, jitter);*/
    }

    private void initJitter(RtpPacket packet) {
        this.currentTransitDelayTime = rtpClock.getLocalRtpTime() - packet.getTimeStamp();

        /*logger.debug("[initJitter] currentTransitDelay: {}, rtpClock.getLocalRtpTime(): {}, packet.getTimeStamp(): {}",
                currentTransitDelay, rtpClock.getLocalRtpTime(), packet.getTimeStamp()
        );*/
    }

    public void estimateRtt(long receiptDate, long lastSR, long delaySinceSR) {
        TimeStamp receiptNtp = TimeStamp.getNtpTime(receiptDate);
        long receiptNtpTime = NtpUtils.calculateLastSrTimestamp(receiptNtp.getSeconds(), receiptNtp.getFraction());
        long delay = receiptNtpTime - lastSR - delaySinceSR;
        this.roundTripDelay = (delay > 4294967L) ? RTP_SEQ_MOD : (int) ((delay * 1000L) >> 16);

        /*if (logger.isTraceEnabled()) {
            logger.trace("rtt=" + receiptNtpTime + " - " + lastSR + " - " + delaySinceSR + " = " + delay + " => "
                    + this.roundTripDelay + "ms");
        }*/
        logger.debug("rtt=" + receiptNtpTime + " - " + lastSR + " - " + delaySinceSR + " = " + delay + " => "
                + this.roundTripDelay + "ms");
    }

    private void initSequence(int sequence) {
        this.firstSequenceNumber = sequence;
        this.highestSequence = sequence;
        this.badSequence = RTP_SEQ_MOD + 1; // so seq != bad_seq
        this.sequenceCycle = 0;
        this.receivedPrior = 0;
        this.expectedPrior = 0;
    }

    private boolean updateSequence(int sequence) {
        int delta = Math.abs(sequence - this.highestSequence);

        /*
         * Source is not valid until MIN_SEQUENTIAL packets with
         * sequential sequence numbers have been received.
         */
        if (this.probation > 0) {
            // packet is in sequence
            if (sequence == this.highestSequence + 1) {
                this.probation--;
                this.highestSequence = sequence;

                if (this.probation == 0) {
                    initSequence(sequence);
                    return true;
                }
            } else {
                this.probation = MIN_SEQUENTIAL - 1;
                this.highestSequence = sequence;
            }
            return false;
        } else if (delta < MAX_DROPOUT) {
            // in order, with permissible gap
            if (sequence < this.highestSequence) {
                // sequence number wrapped - count another 64k cycle
                this.sequenceCycle += RTP_SEQ_MOD;
            }
            this.highestSequence = sequence;
        } else if (delta <= RTP_SEQ_MOD - MAX_MISORDER) {
            // the sequence number made a very large jump
            if (sequence == this.badSequence) {
                /*
                 * Two sequential packets -- assume that the other side
                 * restarted without telling us so just re-sync (i.e., pretend
                 * this was the first packet).
                 */
                initSequence(sequence);
            } else {
                this.badSequence = (sequence + 1) & (RTP_SEQ_MOD - 1);
                return false;
            }
        } else {
            // duplicate or reordered packet
            logger.warn("duplicate or reordered packet");
        }
        return true;
    }

    public void onReceiveRtp(RtpPacket packet) {
        if (validateSequence((int) packet.getSeqNum())) {
            this.receivedSinceSR++;
            this.receivedPackets++;
            this.receivedOctets += packet.getPayloadLength();

            if (this.lastPacketOnReceivedTime > 0) {
                estimateJitter(packet);
            } else {
                initJitter(packet);
            }
            this.lastPacketOnReceivedTime = rtpClock.getLocalRtpTime();
        }
    }

    private boolean validateSequence(int sequence) {
        /*
         * When a new source is heard for the first time, that is, its SSRC
         * identifier is not in the table (see Section 8.2), and the per-source
         * state is allocated for it, s->probation is set to the number of
         * sequential packets required before declaring a source valid
         * (parameter MIN_SEQUENTIAL) and other variables are initialized
         */
        if (this.firstSequenceNumber < 0) {
            initSequence(sequence);
            this.highestSequence = sequence - 1;
            this.probation = MIN_SEQUENTIAL;
            return false;
        } else {
            return updateSequence(sequence);
        }
    }

    public void onReceiveSR(RtcpSenderReport senderReport) {
        setDelaySinceLastSrTimestamp();
        this.lastSrTimestamp = NtpUtils.calculateLastSrTimestamp(senderReport.getMswNts(), senderReport.getLswNts());
        this.lastSrReceivedOn = this.wallClock.getCurrentTime();
        this.receivedSinceSR = 0;
    }

    // New
    public long getDelaySinceLastSrTimestamp() {
        return delaySinceLastSrTimestamp;
    }

    // New
    public void setDelaySinceLastSrTimestamp() {
        if (this.lastSrTimestamp > 0) {
            TimeStamp curTime = TimeStamp.getCurrentTime();
            long curSeconds = curTime.getSeconds();
            long curFraction = curTime.getFraction();
            long curTimeStamp = NtpUtils.calculateLastSrTimestamp(curSeconds, curFraction);
            this.delaySinceLastSrTimestamp = curTimeStamp - lastSrTimestamp;
        }
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
