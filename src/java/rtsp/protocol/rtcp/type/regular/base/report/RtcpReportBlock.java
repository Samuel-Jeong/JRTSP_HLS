package rtsp.protocol.rtcp.type.regular.base.report;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import rtsp.protocol.base.ByteUtil;

public class RtcpReportBlock {

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int LENGTH = 24; // bytes

    // SSRC
    private long ssrc; // (32 bits, unsigned int)

    // - Fraction of the RTP data packets from source that it assumes to be lost
    //      since it sent the previous SR or RR packet.
    // - Expressed as a fixed point number with the binary point at the left edge of the field.
    // - This fraction is defined to be the number of packets lost
    //      divided by the number of packets expected, as defined in the next paragraph.
    //      (An implementation is shown in Appendix A.3.)
    // - If the loss is negative due to duplicates, the fraction lost is set to zero.
    // - Note that a receiver cannot tell whether any packets were lost after the last one received,
    //      and that there will be no reception report block issued for a source
    // - If all packets from that source sent during the last reporting interval have been lost.
    private short fraction; // (8 bits, x/256)

    // Cumulative number of packets lost
    // - Total number of RTP packets from source that have been lost since the beginning of reception.
    // - Packets that arrive late ar not counted as lost, and the loss may be negative if there are duplicates.
    private int cnpl; // (24 bits)

    // Extended Highest Sequence Number
    // - The least significant 16 bits (Sequence number cycles count)
    //      : The highest sequence number received in an RTP data packet from source
    // - The most significant 16 bits (Highest sequence number received)
    //      : The corresponding count of sequence number cycles which may be maintained according to the algorithm in Appendix A.1.
    // - Note that different receivers within the same session will generate different extensions
    //      to the sequence number if their start times differ significantly.
    private long ehsn; // (32 bits, unsigned int)

    // inter-arrival Jitter
    // - An estimate of the statistical variance of the RTP data packet inter-arrival time,
    //      measured in timestamp units and expressed as an unsigned integer.
    // - The difference in the "relative transit time" for the two packets.
    // - The relative transit time is the difference between a packet's RTP timestamp and
    // - The receiver's clock at the time of arrival, measured in the same units.
    // - If Si is the RTP timestamp from packet i,
    //      and Ri is the time of arrival in RTP timestamp units for packet i,
    //      then for two packets i and j, D may be expressed as
    //          D(i,j)=(Rj-Ri)-(Sj-Si)=(Rj-Sj)-(Ri-Si)
    // - The interarrival jitter is calculated continuously as each data packet i is received from source SSRC_n,
    // using this difference D for that packet and the previous packet i-1 in order of arrival (not necessarily in sequence),
    // according to the formula
    //          J=J+(|D(i-1,i)|-J)/16           > Whenever a reception report is issued, the current value of J is sampled.
    // - The jitter calculation is prescribed here to allow profile-independent monitors
    //      to make valid interpretations of reports coming from different implementations.
    // - This algorithm is the optimal first- order estimator and the gain parameter 1/16
    //      gives a good noise reduction ratio while maintaining a reasonable rate of convergence [11].
    //      (A sample implementation is shown in Appendix A.8.)
    // - 지터 값이 크면, 패킷 전송 불안정성이 크다는 의미이므로, 보다 큰 패킷 버퍼가 필요
    private long iaj; // (32 bits, unsigned int)

    // Last SR
    // - The middle 32 bits out of 64 in the NTP timestamp (as explained in Section 4)
    //      received as part of the most recent RTCP sender report (SR) packet from source SSRC_n.
    // - If no SR has been received yet, the field is set to zero.
    // - 제일 마지막에 수신한 NTP Timestamp의 일부, 64비트의 NTP Timestamp의 제일 가운데의 32비트
    private long lsr; // (32 bits, unsigned int)

    // Delay since Last SR
    // - The delay, expressed in units of 1/65536 seconds,
    //      between receiving the last SR packet from source SSRC_n and sending this reception report block.
    // - If no SR packet has been received yet from SSRC_n, the DLSR field is set to zero.
    // - 제일 마지막에 SR을 수신한 이후의 경과 시간
    private long dlsr; // (32 bits, unsigned int)

    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpReportBlock(long ssrc, short fraction, int cnpl, long ehsn, long iaj, long lsr, long dlsr) {
        this.ssrc = (int) ssrc;
        this.fraction = (byte) fraction;
        this.cnpl = cnpl;
        this.ehsn = (int) ehsn;
        this.iaj = (int) iaj;
        this.lsr = (int) lsr;
        this.dlsr = (int) dlsr;
    }

    public RtcpReportBlock() {}

    public RtcpReportBlock(byte[] data) {
        if (data.length >= LENGTH) {
            int index = 0;

            byte[] ssrcData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, ssrcData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] ssrcData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(ssrcData, 0, ssrcData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            ssrc = ByteUtil.bytesToLong(ssrcData2, true);
            index += ByteUtil.NUM_BYTES_IN_INT;

            byte[] fData = new byte[1];
            System.arraycopy(data, index, fData, 0, 1);
            byte[] fData2 = new byte[ByteUtil.NUM_BYTES_IN_SHORT];
            System.arraycopy(fData, 0, fData2, 1, 1);
            fraction = ByteUtil.bytesToShort(fData2, true);
            index += 1;

            byte[] cData = new byte[3];
            System.arraycopy(data, index, cData, 0, 3);
            byte[] cData2 = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(cData, 0, cData2, 1, 3);
            cnpl = ByteUtil.bytesToInt(cData2, true);
            index += 3;

            byte[] ehsnData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, ehsnData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] ehsnData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(ehsnData, 0, ehsnData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            ehsn = ByteUtil.bytesToLong(ehsnData2, true);
            index += ByteUtil.NUM_BYTES_IN_INT;

            byte[] jData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, jData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] jData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(jData, 0, jData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            iaj = ByteUtil.bytesToLong(jData2, true);
            index += ByteUtil.NUM_BYTES_IN_INT;

            byte[] lsrData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, lsrData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] lsrData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(lsrData, 0, lsrData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            lsr = ByteUtil.bytesToLong(lsrData2, true);
            index += ByteUtil.NUM_BYTES_IN_INT;

            byte[] dlsrData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, dlsrData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] dlsrData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(dlsrData, 0, dlsrData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            dlsr = ByteUtil.bytesToLong(dlsrData2, true);
        }
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    public byte[] getByteData() {
        byte[] data = new byte[LENGTH];
        int index = 0;

        byte[] ssrcData = ByteUtil.intToBytes((int) ssrc, true);
        System.arraycopy(ssrcData, 0, data, index, ssrcData.length);
        index += ssrcData.length;

        // F & C
        int fc = 0;
        fc |= fraction;
        fc <<= 0x24;
        fc |= cnpl;
        byte[] fcData = ByteUtil.intToBytes(fc, true);
        System.arraycopy(fcData, 0, data, index, fcData.length);
        index += fcData.length;

        // EHSN
        byte[] ehsnData = ByteUtil.intToBytes((int) ehsn, true);
        System.arraycopy(ehsnData, 0, data, index, ehsnData.length);
        index += ehsnData.length;

        // J
        byte[] jData = ByteUtil.intToBytes((int) iaj, true);
        System.arraycopy(jData, 0, data, index, jData.length);
        index += jData.length;

        // LSR
        byte[] lsrData = ByteUtil.intToBytes((int) lsr, true);
        System.arraycopy(lsrData, 0, data, index, lsrData.length);
        index += lsrData.length;

        // SLSR
        byte[] dlsrData = ByteUtil.intToBytes((int) dlsr, true);
        System.arraycopy(dlsrData, 0, data, index, dlsrData.length);

        return data;
    }

    public void setData(long ssrc, byte fraction, int cnpl, long ehsn, long iaj, long lsr, long dlsr) {
        this.ssrc = ssrc;
        this.fraction = fraction;
        this.cnpl = cnpl;
        this.ehsn = ehsn;
        this.iaj = iaj;
        this.lsr = lsr;
        this.dlsr = dlsr;
    }

    public long getSsrc() {
        return ssrc;
    }

    public void setSsrc(long ssrc) {
        this.ssrc = ssrc;
    }

    public short getFraction() {
        return fraction;
    }

    public void setFraction(short fraction) {
        this.fraction = fraction;
    }

    public int getCnpl() {
        return cnpl;
    }

    public void setCnpl(int cnpl) {
        this.cnpl = cnpl;
    }

    public long getEhsn() {
        return ehsn;
    }

    public void setEhsn(long ehsn) {
        this.ehsn = ehsn;
    }

    public long getIaj() {
        return iaj;
    }

    public void setIaj(long iaj) {
        this.iaj = iaj;
    }

    public long getLsr() {
        return lsr;
    }

    public void setLsr(long lsr) {
        this.lsr = lsr;
    }

    public long getDlsr() {
        return dlsr;
    }

    public void setDlsr(long dlsr) {
        this.dlsr = dlsr;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
