package rtsp.protocol.rtcp.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.protocol.rtcp.base.RtcpFormat;
import rtsp.protocol.rtcp.base.RtcpType;
import rtsp.protocol.rtcp.type.regular.*;
import rtsp.protocol.rtcp.type.regular.base.RtcpHeader;

import java.util.Arrays;

public class RtcpPacket {

    private static final Logger logger = LoggerFactory.getLogger(RtcpPacket.class);

    /**
     * - Reference
     * https://datatracker.ietf.org/doc/html/rfc1889
     * https://www4.cs.fau.de/Projects/JRTP/pmt/node82.html
     * https://www.freesoft.org/CIE/RFC/1889/13.htm
     *
     * - Each RTCP packet carries in its header one of the following packet type codes:
     * 1) 200 = SR (Sender Report packet)
     * 2) 201 = RR (Receiver Report packet)
     * 3) 202 = SDES (Source Description packet)
     * 4) 203 = BYE (Goodbye packet)
     * 5) 204 = APP (Application-defined packet)
     *
     * - RTCP 정의
     * 1) RTP 미디어 스트림에 관한 외부 정보의 송수신이나 제어를 하는 방법을 제공
     * 2) 보통 RTP에서 사용하는 Transport Layer의 포트 번호보다 1 큰 포트 번호를 사용
     *
     * - RTCP 기능
     * 1) 송신 미디어에 관한 정보 통지 (Sender Report)
     * 2) 수신 미디어에 관한 통계 정보 통지 (Receiver Report)
     * 3) 미디어 소스의 정보 통지 (Source Description)
     * 4) 세션으로부터의 이탈 통지 (Goodbye)
     * 5) 어플리케이션별 기능 정의 및 제공 (Application Defined)
     *
     * - RTCP 패킷을 통한 미디어 스트림에 관한 주요 통계 정보
     * 1) 왕복 지연
     *      - Tr : 수신 시각을 NTP Timestamp 로서 취득 > 중앙의 32 비트를 Tr 로 정의
     *      - Lsr : 송신 시각을 NTP Timestamp 로서 취득 > 중앙의 32 비트를 Lsr 로 정의
     *      - Dlsr : 가장 마지막 SR 패킷을 받은 이후 경과 시간 > 32 비트 Timestamp 을 Dslr 로 정의
     *      - 왕복 지연 시간(추정 시간) = Tr - Lsr - Dlsr
     * 2) 패킷 수신 간격 지터 (불안정성, 편차, 패킷 간의 간격이 일정하지 않는 현상)
     * 3) 패킷 손실
     *
     * - 주의 사항
     * 1) RTCP 자신의 패킷에 따라 RTP 미디어 스트림의 패킷 송수신의 품질에 영향을 주지 않도록 주의할 필요가 있다.
     *      - RTCP 패킷이 RTP 미디어 스트림 패킷에 대해서 점유하는 비율은 최대 5%까지로 하는 것을 추천
     *
     *
     *      Request:
     *           Message that requires acknowledgement
     *
     *      Command:
     *           Message that forces the receiver to an action
     *
     *      Indication:
     *           Message that reports a situation
     *
     *      Notification:
     *           Message that provides a notification that an event has
     *           occurred.  Notifications are commonly generated in
     *           response to a Request.
     */

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int PACKET_MULTIPLE = 4; // 32 bits word

    private RtcpHeader rtcpHeader = null;
    private RtcpFormat rtcpFormat = null;
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpPacket(RtcpHeader rtcpHeader, RtcpFormat rtcpFormat) {
        this.rtcpHeader = rtcpHeader;
        this.rtcpFormat = rtcpFormat;
    }

    public RtcpPacket() {}

    public RtcpPacket(byte[] data) {
        if (data.length >= RtcpHeader.LENGTH_SDES) {
            int headerLength;
            byte[] headerData = new byte[RtcpHeader.LENGTH];
            System.arraycopy(data, 0, headerData, 0, RtcpHeader.LENGTH);
            rtcpHeader = new RtcpHeader(headerData);
            headerLength = headerData.length;

            if (rtcpHeader.getPacketType() == RtcpType.SOURCE_DESCRIPTION) {
                byte[] sdesHeaderData = new byte[RtcpHeader.LENGTH_SDES];
                System.arraycopy(data, 0, sdesHeaderData, 0, RtcpHeader.LENGTH_SDES);
                rtcpHeader = new RtcpHeader(sdesHeaderData);
                headerLength = sdesHeaderData.length;
            }

            int remainDataLength = data.length - headerLength;
            byte[] remainData = new byte[remainDataLength];
            System.arraycopy(data, headerLength, remainData, 0, remainDataLength);
            rtcpFormat = getRtcpFormatByByteData(rtcpHeader.getPacketType(), remainData);
        }
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    public static RtcpFormat getRtcpFormatByByteData(int packetType, byte[] data) {
        RtcpFormat rtcpFormat = null;

        switch (packetType) {
            case RtcpType.SENDER_REPORT:
                rtcpFormat = new RtcpSenderReport(data);
                break;
            case RtcpType.RECEIVER_REPORT:
                rtcpFormat = new RtcpReceiverReport(data);
                break;
            case RtcpType.SOURCE_DESCRIPTION:
                rtcpFormat = new RtcpSourceDescription(data);
                break;
            case RtcpType.GOOD_BYE:
                rtcpFormat = new RtcpBye(data);
                break;
            case RtcpType.APPLICATION_DEFINED:
                rtcpFormat = new RtcpApplicationDefined(data);
                break;
            default:
                logger.warn("UNKNOWN RTCP TYPE ({})", packetType);
                break;
        }

        return rtcpFormat;
    }

    public byte[] getData() {
        if (rtcpHeader == null || rtcpFormat == null) {
            return null;
        }

        byte[] data;
        if (rtcpHeader.getPacketType() == RtcpType.SOURCE_DESCRIPTION) {
            data = new byte[RtcpHeader.LENGTH_SDES];
        } else {
            data = new byte[RtcpHeader.LENGTH];
        }
        int index = 0;

        // HEADER
        byte[] headerData = rtcpHeader.getData();
        System.arraycopy(headerData, 0, data, index, headerData.length);
        index += headerData.length;

        // BODY + PADDING
        byte[] rtcpFormatData = rtcpFormat.getData();
        if (rtcpFormatData != null && rtcpFormatData.length > 0) {
            // Check the validation of the format data size > The size must be a 32 bits word multiple!
            int rtcpFormatDataLength = rtcpFormatData.length;
            int paddingBytes = rtcpHeader.getPaddingBytes();
            if (paddingBytes > 0) {
                byte[] newRtcpFormatData = new byte[rtcpFormatDataLength + paddingBytes];
                Arrays.fill(newRtcpFormatData, (byte) 0);
                System.arraycopy(rtcpFormatData, 0, newRtcpFormatData, 0, rtcpFormatDataLength);
                rtcpFormatData = newRtcpFormatData;
                rtcpFormatDataLength = rtcpFormatData.length;
            }

            int newHeaderLength;
            if (rtcpHeader.getPacketType() == RtcpType.SOURCE_DESCRIPTION) {
                newHeaderLength = RtcpHeader.LENGTH_SDES;
            } else {
                newHeaderLength = RtcpHeader.LENGTH;
            }
            byte[] newData = new byte[newHeaderLength + rtcpFormatDataLength];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;

            System.arraycopy(rtcpFormatData, 0, data, index, rtcpFormatDataLength);
        }

        return data;
    }

    public void setData(RtcpFormat rtcpFormat) {
        if (rtcpFormat == null) { return; }

        this.rtcpFormat = rtcpFormat;
    }

    public RtcpHeader getRtcpHeader() {
        return rtcpHeader;
    }

    public void setRtcpHeader(RtcpHeader rtcpHeader) {
        this.rtcpHeader = rtcpHeader;
    }

    public RtcpFormat getRtcpFormat() {
        return rtcpFormat;
    }

    public void setRtcpFormat(RtcpFormat rtcpFormat) {
        this.rtcpFormat = rtcpFormat;
    }

    public static RtcpPacketPaddingResult getPacketLengthByBytes(int bytes, boolean isSdes) {
        RtcpPacketPaddingResult rtcpPacketPaddingResult = new RtcpPacketPaddingResult();

        // 1) 데이터가 없으므로 0 을 반환
        if (bytes <= 0) { return rtcpPacketPaddingResult; }

        // 헤더가 8 바이트 고정이므로 기본적으로 패킷으로 구성되려면
        // 헤더를 포함한 전체 데이터 크기(Header + Body + Padding)는 8 바이트 초과되어야만 한다.
        // SDES 는 4 바이트 초과
        if (isSdes) {
            bytes += RtcpHeader.LENGTH_SDES;
        } else {
            bytes += RtcpHeader.LENGTH;
        }

        int remainderBytesByMultiple = bytes % PACKET_MULTIPLE;
        if (remainderBytesByMultiple != 0) {
            int paddingLength = PACKET_MULTIPLE - remainderBytesByMultiple;
            bytes += paddingLength;
            rtcpPacketPaddingResult.setPaddingBytes(paddingLength);
            rtcpPacketPaddingResult.setPadding(true);
        }

        int dividedBytesByMultiple = bytes / PACKET_MULTIPLE;
        // 2) 헤더를 포함한 전체 데이터 크기가 8 바이트 초과인 경우, 1 감소한 값을 반환
        if (dividedBytesByMultiple > 1) {
            rtcpPacketPaddingResult.setLength(dividedBytesByMultiple - 1);
            return rtcpPacketPaddingResult;
        }
        // 3) 헤더만 포함되어있거나 의미없는 데이터가 포함된 경우, 0 을 반환
        else {
            return rtcpPacketPaddingResult;
        }
    }

    public static int getRemainBytesByPacketLength(int length, boolean isSdes) { // except for header length
        if (length <= 0) { return 0; }
        int resultLength = (length + 1) * 4;
        if (isSdes) {
            return resultLength - RtcpHeader.LENGTH_SDES;
        } else {
            return resultLength - RtcpHeader.LENGTH;
        }
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
