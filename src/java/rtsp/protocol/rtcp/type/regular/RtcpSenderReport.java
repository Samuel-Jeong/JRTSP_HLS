package rtsp.protocol.rtcp.type.regular;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import rtsp.protocol.base.ByteUtil;
import rtsp.protocol.rtcp.base.RtcpFormat;
import rtsp.protocol.rtcp.type.regular.base.report.RtcpReportBlock;

import java.util.ArrayList;
import java.util.List;

public class RtcpSenderReport extends RtcpFormat {

    /**
     *  0                   1                   2                   3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |              NTP timestamp, most significant word             | sender
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ info
     * |             NTP timestamp, least significant word             |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                         RTP timestamp                         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                     sender's packet count                     |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                      sender's octet count                     |
     * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
     * |                 SSRC_1 (SSRC of first source)                 | report
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ block
     * | fraction lost |       cumulative number of packets lost       |   1
     * -+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |           extended highest sequence number received           |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                      interarrival jitter                      |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                         last SR (LSR)                         |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                   delay since last SR (DLSR)                  |
     * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
     * |                 SSRC_2 (SSRC of second source)                | report
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+ block
     * :                               ...                             :   2
     * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
     * |                  profile-specific extensions                  |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int MIN_LENGTH = 20; // bytes

    // - The npt timestamp that indicates the point of time measured in wall clock time when this report was sent.
    // - In combination with timestamps returned in reception reports from the respective receivers,
    // - it can be used to estimate the round-trip propagation time to and from the receivers.
    // - 1900/1/1 을 기준으로 계산
    // - NTP 타임스탬프는 RFC 1305에서 규정되어 있는 NTP에서 네트워크 상의 서버로부터 시간 정보를 얻을 때에 이용되고 있는 64비트 길이의 시간 포맷을 사용
    // - 32비트로 표현할 수 있는 2^32초는 약 136년에 해당하기 때문에, NTP 타임스탬프는 2036년에는 overflow하지만,
    //      RTP/RTCP에 있어서는 프로토콜 동작 상으로는 상대적으로 관계만이 중요하기 때문에 적절하게 overflow를 고려해서 구현한 시스템에 있어서는, 2036년 에도 논리 상으로는 문제가 되지 않는다.
    //      또한, RTP/RTCP의 시스템에서는, NTP 타임스탬프의 64비트 전부를 이용하는 것이 아니고,
    //      정수 부분의 하위 16비트와 소수 부분의 상위 16비트분의 총 32비티를 상대적인 시간을 계산하는데 이용한다.
    //      왕복 지연 시간의 계측에 이용되는 SR 패킷 및 RR 패킷의 레포트 블록에 포함된 LSR이나 DSLR에서는, 이 32비트의 포맷이 이용된다.
    // - NTP는 네트워크상의 시간 관리 서버로부터 시간 정보를 얻는 것으로, 시스템 시간을 절대 시간에 동기화시키는 것이 가능하지만,
    //      RTP/RTCP의 시스템은 반드시, NTP를 동작 시킬 필요는 없다.
    //      시스템의 기동으로부터의 경과 시간을 NTP 타임스탬프와 동일한 스케줄로 사용하는 것으로,
    //      RTP/RTCP가 제공하는 왕복 지원의 측정이나,미디어간의 동기당의 기능을 이용할 수 있다.
    //      그러나, 다른 시스템 사이에서의 정도가 높은 동기화를 취할 필요가 있는 경우에는 NTP에서 제공된 절대 시간을 이용하는 것이 하나의 방법으로서 유효하다.
    // - 또한, 상대 시간도 포함해서 RTP/RTCP를 이용하는 시스템에 있어서 모든 블록의 관리를 하지 않는 경우도, RTP/RTCP를 이용하는 것이 가능하다.
    //      이 경우에는 NTP 타임 스탬프에 있는 부분에는 "0"을 설정하게 된다.
    //      다만, 클록을 이용하지 않는 경우에는, 미디어 스트림간의 동기나, 왕복지연 시간의 측정등의 기능을 이용할 수 없다.
    private long mswNts = 0; // Most Significant Word Ntp TimeStamp (32 bits) > 상위 비트가 경과한 초의 정수부를 표현
    private long lswNts = 0; // Least Significant Word Ntp TimeStamp (32 bits) > 하위 32비트가 소수점 이하의 초를 1/2^32 초의 단위로 표현

    // The RTP timestamp resembles the same time as the NTP timestamp (above),
    // but is measured in the same units and with the same random offset
    // as the RTP timestamps in data packets.
    // This correspondence may be used for intra- and inter-media synchronisation
    // for sources whose NTP timestamps are synchronised,
    // and may be used by media-independent receivers to estimate the nominal RTP clock frequency.
    private long rts = 0; // Rtp TimeStamp (32 bits)

    // The sender's packet count totals up the number of RTP data packets
    // transmitted by the sender since joining the RTP session.
    // This field can be used to estimate the average data packet rate.
    // The total number of RTP data packets transmitted by the sender
    // since starting transmission up until the time this SR packet was generated.
    // The count is reset if the sender changes its SSRC identifier.
    private long spc = 0; // Sender Packet Count total (32 bits)

    // The total number of payload octets (i.e., not including the header or any padding)
    // transmitted in RTP data packets by the sender since starting up transmission.
    // This field can be used to estimate the average payload data rate.
    private long soc = 0; // Sender Octet Count total (32 bits)

    // Report Block List
    private List<RtcpReportBlock> rtcpReportBlockList = null;

    // Profile-specific extensions
    private byte[] profileSpecificExtensions = null;
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpSenderReport(long mswNts, long lswNts, long rts, long spc, long soc,
                            List<RtcpReportBlock> rtcpReportBlockList, byte[] profileSpecificExtensions) {
        this.mswNts = (int) mswNts;
        this.lswNts = (int) lswNts;
        this.rts = (int) rts;
        this.spc = (int) spc;
        this.soc = (int) soc;
        this.rtcpReportBlockList = rtcpReportBlockList;
        this.profileSpecificExtensions = profileSpecificExtensions;
    }

    public RtcpSenderReport() {}

    public RtcpSenderReport(byte[] data) {
        int dataLength = data.length;
        if (dataLength >= MIN_LENGTH) {
            int index = 0;
            rtcpReportBlockList = null;

            // NTS
            byte[] mswNtsData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, mswNtsData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] mswNtsData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(mswNtsData, 0, mswNtsData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            mswNts = ByteUtil.bytesToLong(mswNtsData2, true);
            index += ByteUtil.NUM_BYTES_IN_INT;

            byte[] lswNtsData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, lswNtsData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] lswNtsData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(lswNtsData, 0, lswNtsData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            lswNts = ByteUtil.bytesToLong(lswNtsData2, true);
            index += ByteUtil.NUM_BYTES_IN_INT;

            // RTS > RtpPacket.getTimeStamp()
            byte[] rtsData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, rtsData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] rtsData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(rtsData, 0, rtsData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            rts = ByteUtil.bytesToLong(rtsData2, true);
            index += ByteUtil.NUM_BYTES_IN_INT;

            // SPC > Get by Network Statistics
            byte[] spcData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, spcData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] spcData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(spcData, 0, spcData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            spc = ByteUtil.bytesToLong(spcData2, true);
            index += ByteUtil.NUM_BYTES_IN_INT;

            // SOC > Get by Network Statistics
            byte[] socData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, socData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] socData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(socData, 0, socData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            soc = ByteUtil.bytesToLong(socData2, true);
            index += ByteUtil.NUM_BYTES_IN_INT;

            if (dataLength > MIN_LENGTH) {
                rtcpReportBlockList = new ArrayList<>();

                // ReportBlock
                int curBlockIndex = index;
                while (curBlockIndex < dataLength) {
                    if (dataLength - curBlockIndex < RtcpReportBlock.LENGTH) { break; }

                    byte[] curBlockData = new byte[RtcpReportBlock.LENGTH];
                    System.arraycopy(data, curBlockIndex, curBlockData, 0, RtcpReportBlock.LENGTH);
                    RtcpReportBlock rtcpReceiverRtcpReportBlock = new RtcpReportBlock(curBlockData);
                    rtcpReportBlockList.add(rtcpReceiverRtcpReportBlock);
                    curBlockIndex += RtcpReportBlock.LENGTH;
                }

                // Profile Specific Extensions
                index = curBlockIndex;
                int remainLength = dataLength - index;
                if (remainLength > 0) {
                    profileSpecificExtensions = new byte[remainLength];
                    System.arraycopy(data, index, profileSpecificExtensions, 0, remainLength);
                }
            }
        }
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    @Override
    public byte[] getData() {
        byte[] data = new byte[MIN_LENGTH];
        int index = 0;

        // NTS
        byte[] ntsData = new byte[ByteUtil.NUM_BYTES_IN_LONG];
        byte[] mswNtsData = ByteUtil.intToBytes((int) mswNts, true);
        System.arraycopy(mswNtsData, 0, ntsData, 0, mswNtsData.length);
        byte[] lswNtsData = ByteUtil.intToBytes((int) lswNts, true);
        System.arraycopy(lswNtsData, 0, ntsData, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
        System.arraycopy(ntsData, 0, data, index, ntsData.length);
        index += ntsData.length;

        // RTS
        byte[] rtsData = ByteUtil.intToBytes((int) rts, true);
        System.arraycopy(rtsData, 0, data, index, rtsData.length);
        index += rtsData.length;

        // SPC
        byte[] spcData = ByteUtil.intToBytes((int) spc, true);
        System.arraycopy(spcData, 0, data, index, spcData.length);
        index += spcData.length;

        // SOC
        byte[] socData = ByteUtil.intToBytes((int) soc, true);
        System.arraycopy(socData, 0, data, index, socData.length);
        index += socData.length;

        // Report Block
        if (rtcpReportBlockList != null && !rtcpReportBlockList.isEmpty()) {
            byte[] newData = new byte[data.length + (rtcpReportBlockList.size() * RtcpReportBlock.LENGTH)];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;

            for (RtcpReportBlock rtcpReceiverRtcpReportBlock : rtcpReportBlockList) {
                if (rtcpReceiverRtcpReportBlock == null) { continue; }

                byte[] curReportBlockData = rtcpReceiverRtcpReportBlock.getByteData();
                System.arraycopy(curReportBlockData, 0, data, index, curReportBlockData.length);
                index += curReportBlockData.length;
            }
        }

        // Profile Specific Extenstions
        if (profileSpecificExtensions != null && profileSpecificExtensions.length > 0) {
            byte[] newData = new byte[data.length + profileSpecificExtensions.length];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;

            System.arraycopy(profileSpecificExtensions, 0, data, index, profileSpecificExtensions.length);
        }

        return data;
    }

    public void setData(long mswNts, long lswNts, long rts, long spc, long soc,
                        List<RtcpReportBlock> rtcpReportBlockList, byte[] profileSpecificExtensions) {
        this.mswNts = mswNts;
        this.lswNts = lswNts;
        this.rts = rts;
        this.spc = spc;
        this.soc = soc;
        this.rtcpReportBlockList = rtcpReportBlockList;
        this.profileSpecificExtensions = profileSpecificExtensions;
    }

    public long getMswNts() {
        return mswNts;
    }

    public void setMswNts(long mswNts) {
        this.mswNts = mswNts;
    }

    public long getLswNts() {
        return lswNts;
    }

    public void setLswNts(long lswNts) {
        this.lswNts = lswNts;
    }

    public long getRts() {
        return rts;
    }

    public void setRts(long rts) {
        this.rts = rts;
    }

    public long getSpc() {
        return spc;
    }

    public void setSpc(long spc) {
        this.spc = spc;
    }

    public long getSoc() {
        return soc;
    }

    public void setSoc(long soc) {
        this.soc = soc;
    }

    public void setSoc(int soc) {
        this.soc = soc;
    }

    public List<RtcpReportBlock> getReportBlockList() {
        return rtcpReportBlockList;
    }

    public RtcpReportBlock getReportBlockByIndex(int index) {
        if (rtcpReportBlockList == null || index < 0 || index >= rtcpReportBlockList.size()) { return null; }
        return rtcpReportBlockList.get(index);
    }

    public void setReportBlockList(List<RtcpReportBlock> rtcpReportBlockList) {
        this.rtcpReportBlockList = rtcpReportBlockList;
    }

    public byte[] getProfileSpecificExtensions() {
        return profileSpecificExtensions;
    }

    public void setProfileSpecificExtensions(byte[] profileSpecificExtensions) {
        this.profileSpecificExtensions = profileSpecificExtensions;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
