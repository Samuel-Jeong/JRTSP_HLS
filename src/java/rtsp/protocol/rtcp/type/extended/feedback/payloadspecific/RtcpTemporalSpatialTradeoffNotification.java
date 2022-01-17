package rtsp.protocol.rtcp.type.extended.feedback.payloadspecific;

import rtsp.protocol.rtcp.type.extended.feedback.RtcpFeedback;
import rtsp.protocol.rtcp.type.extended.feedback.base.RtcpFeedbackMessageHeader;

public class RtcpTemporalSpatialTradeoffNotification extends RtcpFeedback {

    /**
     * Temporal-Spatial Trade-off Notification
     *
     *   The TSTN message is identified by RTCP packet type value PT=PSFB and
     *    FMT=6.
     *
     *    The FCI field SHALL contain one or more TSTN FCI entries.
     *
     *    The content of an FCI entry for the Temporal-Spatial Trade-off
     *    Notification is depicted in Figure 6.  The length of the TSTN message
     *    MUST be set to 2+2*N, where N is the number of FCI entries.
     *    (Syntax of the TSTN)
     *
     *     0                   1                   2                   3
     *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *    |                              SSRC                             |
     *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *    |  Seq nr.      |  Reserved                           | Index   |
     *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     *      SSRC (32 bits): The SSRC of the source of the TSTR that resulted in
     *               this Notification.
     *
     *      Seq nr. (8 bits): The sequence number value from the TSTR that is
     *               being acknowledged.
     *
     *      Reserved (19 bits): All bits SHALL be set to 0 by the sender and
     *               SHALL be ignored on reception.
     *
     *      Index (5 bits): The trade-off value the media sender is using
     *               henceforth.
     *
     *      Informative note: The returned trade-off value (Index) may differ
     *       from the requested one,
     *       for example, in cases where a media encoder cannot tune its trade-off,
     *       or when pre-recorded content is used.
     *
     *  This feedback message is used to acknowledge the reception of a TSTR.
     *   For each TSTR received targeted at the session participant, a TSTN
     *    FCI entry SHALL be sent in a TSTN feedback message.
     *
     *  A single TSTN
     *    message MAY acknowledge multiple requests using multiple FCI entries.
     *   The index value included SHALL be the same in all FCI entries of the
     *    TSTN message.
     *
     *  Including a FCI for each requestor allows each
     *    requesting entity to determine that the media sender received the
     *    request.
     *  The Notification SHALL also be sent in response to TSTR
     *    repetitions received.
     *
     *  If the request receiver has received TSTR with
     *    several different sequence numbers from a single requestor, it SHALL
     *    only respond to the request with the highest (modulo 256) sequence number.
     *  Note that the highest sequence number may be a smaller
     *    integer value due to the wrapping of the field.
     *    (https://datatracker.ietf.org/doc/html/rfc3550#appendix-A.1)
     *
     *This is not necessarily the same index as requested, as the media sender may need to aggregate
     *    requests from several requesting session participants.
     *    It may also have some other policies or rules that limit the selection.
     *
     *   Within the common packet header for feedback messages (as defined in
     *    section 6.1 of [RFC4585]), the "SSRC of packet sender" field
     *    indicates the source of the Notification, and the "SSRC of media
     *    source" is not used and SHALL be set to 0.  The SSRCs of the
     *    requesting entities to which the Notification applies are in the
     *    corresponding FCI entries.
     *
     */

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int MIN_LENGTH = RtcpFeedbackMessageHeader.LENGTH; // bytes


    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpTemporalSpatialTradeoffNotification(RtcpFeedbackMessageHeader rtcpFeedbackMessageHeader) {
        super(rtcpFeedbackMessageHeader);
    }

    public RtcpTemporalSpatialTradeoffNotification() {
    }

    public RtcpTemporalSpatialTradeoffNotification(byte[] data) {
        super(data);
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS


    ////////////////////////////////////////////////////////////

}
