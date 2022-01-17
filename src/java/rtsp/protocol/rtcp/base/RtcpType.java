package rtsp.protocol.rtcp.base;

public class RtcpType {

    public static final short FULL_INTRA_REQUEST = 192;
    public static final short SENDER_REPORT = 200;
    public static final short RECEIVER_REPORT = 201;
    public static final short SOURCE_DESCRIPTION = 202;
    public static final short GOOD_BYE = 203;
    public static final short APPLICATION_DEFINED = 204;
    public static final short RTPFB = 205; // Transport layer FB message
    public static final short PSFB = 205; // Payload-specific FB message
    public static final short AVB = 208;
    public static final short PORT_MAPPING = 210;

}
