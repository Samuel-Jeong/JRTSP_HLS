package rtsp.protocol.rtcp.type.extended.feedback.transportlayer.base;

public class RtcpTransportLayerFeedbackType {

    /**
     *    Assigned in AVPF [RFC4585]:
     *
     *       1:    Generic NACK
     *       31:   reserved for future expansion of the identifier number space
     *
     *    Assigned in this memo:
     *
     *       2:    reserved (see note below)
     *       3:    Temporary Maximum Media Stream Bit Rate Request (TMMBR)
     *       4:    Temporary Maximum Media Stream Bit Rate Notification (TMMBN)
     *
     *    Available for assignment:
     *
     *       0:    unassigned
     *       5-30: unassigned
     */

    public static final int NACK = 1;
    public static final int TMMBR = 3; // Temporary Maximum Media Stream Bit Rate Request
    public static final int TMMBN = 4; // Temporary Maximum Media Stream Bit Rate Notification

}
