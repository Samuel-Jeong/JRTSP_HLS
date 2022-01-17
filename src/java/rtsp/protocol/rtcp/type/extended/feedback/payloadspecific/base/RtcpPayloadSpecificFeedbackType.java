package rtsp.protocol.rtcp.type.extended.feedback.payloadspecific.base;

public class RtcpPayloadSpecificFeedbackType {

    /**
     *       0:     unassigned
     *       1:     Picture Loss Indication (PLI)
     *       2:     Slice Loss Indication (SLI)
     *       3:     Reference Picture Selection Indication (RPSI)
     *       4-14:  unassigned
     *       15:    Application layer FB (AFB) message
     *       16-30: unassigned
     *       31:    reserved for future expansion of the sequence number space
     */

    public static final int UNASSIGNED = 0;
    public static final int PLI = 1; // Picture Loss Indication
    public static final int SLI = 2; // Slice Loss Indication
    public static final int RPSI = 3; // Reference Picture Selection Indication
    public static final int FIR = 4; // Full Intra Request
    public static final int TSTR = 5; // Temporal-Spatial Trade-off Request
    public static final int TSTN = 6; // Temporal-Spatial Trade-off Notification
    public static final int VBCM = 7; // H.271 Video Back Channel Message
    public static final int AFB = 15; // Application layer FB message

}
