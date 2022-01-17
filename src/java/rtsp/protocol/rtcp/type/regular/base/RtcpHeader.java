package rtsp.protocol.rtcp.type.regular.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import rtsp.protocol.base.ByteUtil;
import rtsp.protocol.rtcp.base.RtcpType;
import rtsp.protocol.rtcp.packet.RtcpPacketPaddingResult;

public class RtcpHeader {

    /**
     *  0               1               2               3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |V=2|P|    RC   |       PT      |             length L          |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                         SSRC of sender                        |
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int LENGTH = 8;// 8 bytes (4 + 4(ssrc, unsigned int))
    public static final int LENGTH_SDES = 4;// 8 bytes (4(ssrc is not included))
    // (unsigned integer 변수들 때문에 크게 잡음 (오버플로우 방지하기 위함))

    // VERSION
    // - Identifies the version of RTP, which is the same in RTCP packets as in RTP data packets.
    // - The version defined by this specification is two (2).
    private int version = 0; // (2 bits)

    // PADDING
    // - If the padding bit is set, this RTCP packet contains some additional padding octets
    // at the end which are not part of the control information.
    // - The last octet of the padding is a count of how many padding octets should be ignored.
    // - Padding may be needed by some encryption algorithms with fixed block sizes.
    // - In a compound RTCP packet, padding should only be required on
    // the last individual packet because the compound packet is encrypted as a whole.
    private int padding = 0; // (1 bit)
    transient private int paddingBytes = 0;

    // Resource count
    // 1) The number of reception report blocks contained in this packet.
    //      A value of zero is valid.
    // 2) The number of SSRC/CSRC chunks contained in this SDES packet.
    //      A value of zero is valid but useless.
    private int resourceCount = 0; // (5 bits)

    // PACKET TYPE
    // - Contains the constant 200 to identify this as an RTCP SR packet.
    /**
     * 200 = SR Sender Report packet
     * 201 = RR Receiver Report packet
     * 202 = SDES Source Description packet
     * 203 = BYE Goodbye packet
     * 204 = APP Application-defined packet
     */
    private short packetType = 0; // (8 bits)

    // LENGTH
    // - The length of this RTCP packet in 32-bit words minus one, including the header and any padding.
    // - The offset of one makes zero a valid length and avoids a possible infinite loop in scanning a compound RTCP packet,
    // while counting 32 bit words avoids a validity check for a multiple of 4.
    // - (length + 1) * 4 > 실제 RTCP 패킷의 전체 바이트 수
    // - length 값에 1 을 더하는 이유
    //      > 패킷은 32 비트의 워드 단위로 이루어져있고 이 워드의 배수에서 -1 을 뺀 값이 저장되어 있다.
    //      > 그래서 1 을 더해야 워드 배수가 나온다.
    // - 4 를 곱해야 전체 바이트 수 가 나오는 이유
    //      > 패킷은 1 word(4바이트) 단위로 끊어서 분석되어야 하기 때문에 항상 4(바이트)의 배수이어야 한다.
    // - 그래서 내부 로직에서 RTCP 패킷을 32 비트 워드 단위로 만들어줘야한다. > 패킷 레벨에서 zero padding 필요 (Format 레벨에서는 할 필요 없음, 데이터 원본 유지 필요)
    //      > 패딩한다고 해서 헤더의 padding 값을 true 로 따로 설정하지 않는다. > 이유는 모름 > RTCP 패킷 예제들 보면 패딩은 되어 있지만 true 로 설정되어 있지 않음
    private int length = 0; // (16 bits)

    // SSRC
    // - The synchronization source identifier for the originator of this SR packet.
    private long ssrc = 0; // (32 bits)
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpHeader(int version, int padding, int resourceCount, short packetType, int length, long ssrc) {
        this.version = version;
        this.padding = padding;
        this.resourceCount = resourceCount;
        this.packetType = packetType;
        this.length = length;
        this.ssrc = (int) ssrc;
    }

    public RtcpHeader(int version, int padding, int resourceCount, short packetType, int length) {
        this.version = version;
        this.padding = padding;
        this.resourceCount = resourceCount;
        this.packetType = packetType;
        this.length = length;
    }

    public RtcpHeader(int version, RtcpPacketPaddingResult rtcpPacketPaddingResult, int resourceCount, short packetType, long ssrc) {
        this.version = version;
        //this.padding = rtcpPacketPaddingResult.isPadding()? 1 : 0;
        this.paddingBytes = rtcpPacketPaddingResult.getPaddingBytes();
        this.resourceCount = resourceCount;
        this.packetType = packetType;
        this.length = rtcpPacketPaddingResult.getLength();
        this.ssrc = (int) ssrc;
    }

    public RtcpHeader(int version, RtcpPacketPaddingResult rtcpPacketPaddingResult, int resourceCount, short packetType) {
        this.version = version;
        //this.padding = rtcpPacketPaddingResult.isPadding()? 1 : 0;
        this.paddingBytes = rtcpPacketPaddingResult.getPaddingBytes();
        this.resourceCount = resourceCount;
        this.packetType = packetType;
        this.length = rtcpPacketPaddingResult.getLength();
    }

    public RtcpHeader() {}

    public RtcpHeader(byte[] data) { // big endian
        if (data.length >= LENGTH_SDES) {
            int index = 0;

            // V, P, RC
            byte[] vprcData = new byte[ByteUtil.NUM_BYTES_IN_BYTE];
            System.arraycopy(data, index, vprcData, 0, ByteUtil.NUM_BYTES_IN_BYTE);
            version = (vprcData[0] >>> 0x06) & 0x03;
            padding = (vprcData[0] >>> 0x05) & 0x01;
            resourceCount = vprcData[0] & 0x05;
            index += ByteUtil.NUM_BYTES_IN_BYTE;

            // PT
            byte[] ptData = new byte[ByteUtil.NUM_BYTES_IN_BYTE];
            System.arraycopy(data, index, ptData, 0, ByteUtil.NUM_BYTES_IN_BYTE);
            byte[] ptData2 = new byte[ByteUtil.NUM_BYTES_IN_SHORT];
            System.arraycopy(ptData, 0, ptData2, ByteUtil.NUM_BYTES_IN_BYTE, ByteUtil.NUM_BYTES_IN_BYTE);
            packetType = ByteUtil.bytesToShort(ptData2, true);
            index += ByteUtil.NUM_BYTES_IN_BYTE;

            // LENGTH
            byte[] lengthData = new byte[ByteUtil.NUM_BYTES_IN_SHORT];
            System.arraycopy(data, index, lengthData, 0, 2);
            byte[] lengthData2 = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(lengthData, 0, lengthData2, ByteUtil.NUM_BYTES_IN_SHORT, ByteUtil.NUM_BYTES_IN_SHORT);
            length = ByteUtil.bytesToInt(lengthData2, true);
            index += ByteUtil.NUM_BYTES_IN_SHORT;

            // SSRC
            if (data.length >= LENGTH) {
                byte[] ssrcData = new byte[ByteUtil.NUM_BYTES_IN_INT];
                System.arraycopy(data, index, ssrcData, 0, ByteUtil.NUM_BYTES_IN_INT);
                byte[] ssrcData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
                System.arraycopy(ssrcData, 0, ssrcData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
                ssrc = ByteUtil.bytesToLong(ssrcData2, true);
            }
        }
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    public byte[] getData() {
        byte[] data;
        if (packetType == RtcpType.SOURCE_DESCRIPTION) {
            data = new byte[LENGTH_SDES];
        } else {
            data = new byte[LENGTH];
        }
        int index = 0;

        // V, P, RC
        byte vprc = 0; // version + padding + resource-count
        vprc |= version;
        vprc <<= 0x01;
        vprc |= padding;
        vprc <<= 0x05;
        vprc |= resourceCount;
        byte[] vprcData = { vprc };
        System.arraycopy(vprcData, 0, data, index, ByteUtil.NUM_BYTES_IN_BYTE);
        index += ByteUtil.NUM_BYTES_IN_BYTE;

        // PT
        byte[] ptData = ByteUtil.shortToBytes(packetType, true);
        byte[] ptData2 = { ptData[ByteUtil.NUM_BYTES_IN_BYTE] };
        System.arraycopy(ptData2, 0, data, index, ptData2.length);
        index += ByteUtil.NUM_BYTES_IN_BYTE;

        // LENGTH
        byte[] lengthData = ByteUtil.shortToBytes((short) length, true);
        System.arraycopy(lengthData, 0, data, index, lengthData.length);
        index += 2;

        // SSRC
        if (packetType != RtcpType.SOURCE_DESCRIPTION) {
            byte[] ssrcData = ByteUtil.intToBytes((int) ssrc, true);
            System.arraycopy(ssrcData, 0, data, index, ssrcData.length);
        }

        return data;
    }

    public void setData(int version, int padding, int resourceCount, short packetType, int length, long ssrc) {
        this.version = version;
        this.padding = padding;
        this.resourceCount = resourceCount;
        this.packetType = packetType;
        this.length = length;
        this.ssrc = (int) ssrc;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getPadding() {
        return padding;
    }

    public void setPadding(int padding) {
        this.padding = padding;
    }

    public int getPaddingBytes() {
        return paddingBytes;
    }

    public void setPaddingBytes(int paddingBytes) {
        this.paddingBytes = paddingBytes;
    }

    public int getResourceCount() {
        return resourceCount;
    }

    public void setResourceCount(int resourceCount) {
        this.resourceCount = resourceCount;
    }

    public int getPacketType() {
        return packetType;
    }

    public void setPacketType(short packetType) {
        this.packetType = packetType;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public long getSsrc() {
        return ssrc;
    }

    public void setSsrc(long ssrc) {
        this.ssrc = ssrc;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
