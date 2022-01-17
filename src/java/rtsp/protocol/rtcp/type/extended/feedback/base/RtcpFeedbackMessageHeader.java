package rtsp.protocol.rtcp.type.extended.feedback.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import rtsp.protocol.base.ByteUtil;

public class RtcpFeedbackMessageHeader {

    /**
     * @Reference https://datatracker.ietf.org/doc/html/rfc4585#section-6.1
     *
     *     0                   1                   2                   3
     *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *    |V=2|P|   FMT   |       PT      |          length               |
     *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *    |                  SSRC of packet sender                        |
     *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *    |                  SSRC of media source                         |
     *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *    :            Feedback Control Information (FCI)                 :
     *    :                                                               :
     *
     *            Figure 3: Common Packet Format for Feedback Messages
     *
     *    Feedback message type (FMT): 5 bits
     *       This field identifies the type of the FB message and is
     *       interpreted relative to the type (transport layer, payload-
     *       specific, or application layer feedback).  The values for each of
     *       the three feedback types are defined in the respective sections
     *       below.
     *
     *    Payload type (PT): 8 bits
     *       This is the RTCP packet type that identifies the packet as being
     *       an RTCP FB message.  Two values are defined by the IANA:
     *       > IANA(Internet Assigned Numbers Authority)는 인터넷 할당 번호 관리기관의 약자로 IP 주소,
     *          최상위 도메인 등을 관리하는 단체이다. 현재 ICANN이 관리하고 있다.
     *
     *             Name   | Value | Brief Description
     *          ----------+-------+------------------------------------
     *             RTPFB  |  205  | Transport layer FB message
     *             PSFB   |  206  | Payload-specific FB message
     *
     *    Length: 16 bits
     *       The length of this packet in 32-bit words minus one, including the
     *       header and any padding.  This is in line with the definition of
     *       the length field used in RTCP sender and receiver reports [3].
     *
     *    SSRC of packet sender: 32 bits
     *       The synchronization source identifier for the originator of this
     *       packet.
     *
     *    SSRC of media source: 32 bits
     *       The synchronization source identifier of the media source that
     *       this piece of feedback information is related to.
     *
     *    Feedback Control Information (FCI): variable length
     *       The following three sections define which additional information
     *       MAY be included in the FB message for each type of feedback:
     *       transport layer, payload-specific, or application layer feedback.
     *       Note that further FCI contents MAY be specified in further
     *       documents.
     *
     */

    /**
     *      ACK
     *    feedback
     *      V
     *      :<- - - -  NACK feedback - - - ->//
     *      :
     *      :   Immediate   ||
     *      : Feedback mode ||Early RTCP mode   Regular RTCP mode
     *      :<=============>||<=============>//<=================>
     *      :               ||
     *     -+---------------||---------------//------------------> group size
     *      2               ||
     *       Application-specific FB Threshold
     *          = f(data rate, packet loss, codec, ...)
     *
     */

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int LENGTH = 12;
    private int version = 0; // (2 bits)
    private int padding = 0; // (1 bit)
    transient private int paddingBytes = 0;
    private int feedbackMessageType = 0; // (5 bits)
    private short payloadType = 0; // (8 bits)
    private int length = 0; // (16 bits)
    private long packetSenderSsrc = 0; // (32 bits)
    private long mediaSourceSsrc = 0; // (32 bits)
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpFeedbackMessageHeader(int version, int padding, int paddingBytes,
                                     int feedbackMessageType, short payloadType, int length,
                                     long packetSenderSsrc, long mediaSourceSsrc,
                                     RtcpFeedbackControlInformation feedbackControlInformation) {
        this.version = version;
        this.padding = padding;
        this.paddingBytes = paddingBytes;
        this.feedbackMessageType = feedbackMessageType;
        this.payloadType = payloadType;
        this.length = length;
        this.packetSenderSsrc = packetSenderSsrc;
        this.mediaSourceSsrc = mediaSourceSsrc;
    }

    public RtcpFeedbackMessageHeader() {}

    public RtcpFeedbackMessageHeader(byte[] data) {
        if (data.length == LENGTH) {
            int index = 0;

            // V, P, RC
            byte[] vprcData = new byte[ByteUtil.NUM_BYTES_IN_BYTE];
            System.arraycopy(data, index, vprcData, 0, ByteUtil.NUM_BYTES_IN_BYTE);
            version = (vprcData[0] >>> 0x06) & 0x03;
            padding = (vprcData[0] >>> 0x05) & 0x01;
            feedbackMessageType = vprcData[0] & 0x05;
            index += ByteUtil.NUM_BYTES_IN_BYTE;

            // PT
            byte[] ptData = new byte[ByteUtil.NUM_BYTES_IN_BYTE];
            System.arraycopy(data, index, ptData, 0, ByteUtil.NUM_BYTES_IN_BYTE);
            byte[] ptData2 = new byte[ByteUtil.NUM_BYTES_IN_SHORT];
            System.arraycopy(ptData, 0, ptData2, ByteUtil.NUM_BYTES_IN_BYTE, ByteUtil.NUM_BYTES_IN_BYTE);
            payloadType = ByteUtil.bytesToShort(ptData2, true);
            index += ByteUtil.NUM_BYTES_IN_BYTE;

            // LENGTH
            byte[] lengthData = new byte[ByteUtil.NUM_BYTES_IN_SHORT];
            System.arraycopy(data, index, lengthData, 0, 2);
            byte[] lengthData2 = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(lengthData, 0, lengthData2, ByteUtil.NUM_BYTES_IN_SHORT, ByteUtil.NUM_BYTES_IN_SHORT);
            length = ByteUtil.bytesToInt(lengthData2, true);
            index += ByteUtil.NUM_BYTES_IN_SHORT;

            // Packet Sender SSRC
            byte[] packetSenderSsrcData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, packetSenderSsrcData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] packetSenderSsrcData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(packetSenderSsrcData, 0, packetSenderSsrcData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            packetSenderSsrc = ByteUtil.bytesToLong(packetSenderSsrcData2, true);
            index += packetSenderSsrcData.length;

            // Media Source SSRC
            byte[] mediaSourceSsrcData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, mediaSourceSsrcData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] mediaSourceSsrcDataData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(mediaSourceSsrcData, 0, mediaSourceSsrcDataData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            mediaSourceSsrc = ByteUtil.bytesToLong(mediaSourceSsrcDataData2, true);
        }
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    public byte[] getData() {
        int index = 0;
        byte[] data = new byte[LENGTH];

        byte vpfmt = 0; // version + padding + feedback-message-type
        vpfmt |= version;
        vpfmt <<= 0x01;
        vpfmt |= padding;
        vpfmt <<= 0x05;
        vpfmt |= feedbackMessageType;
        byte[] vpfmtData = { vpfmt };
        System.arraycopy(vpfmtData, 0, data, index, ByteUtil.NUM_BYTES_IN_BYTE);
        index += ByteUtil.NUM_BYTES_IN_BYTE;

        // PT
        byte[] ptData = ByteUtil.shortToBytes(payloadType, true);
        byte[] ptData2 = { ptData[ByteUtil.NUM_BYTES_IN_BYTE] };
        System.arraycopy(ptData2, 0, data, index, ptData2.length);
        index += ByteUtil.NUM_BYTES_IN_BYTE;

        // LENGTH
        byte[] lengthData = ByteUtil.shortToBytes((short) length, true);
        System.arraycopy(lengthData, 0, data, index, lengthData.length);
        index += 2;

        // Packet Sender SSRC
        byte[] packetSenderSsrcData = ByteUtil.intToBytes((int) packetSenderSsrc, true);
        System.arraycopy(packetSenderSsrcData, 0, data, index, packetSenderSsrcData.length);
        index += packetSenderSsrcData.length;

        // Media Source SSRC
        byte[] mediaSourceSsrcData = ByteUtil.intToBytes((int) mediaSourceSsrc, true);
        System.arraycopy(mediaSourceSsrcData, 0, data, index, mediaSourceSsrcData.length);
        index += mediaSourceSsrcData.length;

        return data;
    }

    public void setData(int version, int padding, int paddingBytes,
                        int feedbackMessageType, short payloadType, int length,
                        long packetSenderSsrc, long mediaSourceSsrc) {
        this.version = version;
        this.padding = padding;
        this.paddingBytes = paddingBytes;
        this.feedbackMessageType = feedbackMessageType;
        this.payloadType = payloadType;
        this.length = length;
        this.packetSenderSsrc = packetSenderSsrc;
        this.mediaSourceSsrc = mediaSourceSsrc;
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

    public int getFeedbackMessageType() {
        return feedbackMessageType;
    }

    public void setFeedbackMessageType(int feedbackMessageType) {
        this.feedbackMessageType = feedbackMessageType;
    }

    public short getPayloadType() {
        return payloadType;
    }

    public void setPayloadType(short payloadType) {
        this.payloadType = payloadType;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public long getPacketSenderSsrc() {
        return packetSenderSsrc;
    }

    public void setPacketSenderSsrc(long packetSenderSsrc) {
        this.packetSenderSsrc = packetSenderSsrc;
    }

    public long getMediaSourceSsrc() {
        return mediaSourceSsrc;
    }

    public void setMediaSourceSsrc(long mediaSourceSsrc) {
        this.mediaSourceSsrc = mediaSourceSsrc;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
