package rtsp.protocol.rtcp.type.extended.feedback.payloadspecific;

import rtsp.protocol.rtcp.type.extended.feedback.RtcpFeedback;
import rtsp.protocol.rtcp.type.extended.feedback.base.RtcpFeedbackMessageHeader;

public class RtcpTemporalSpatialTradeoffRequest extends RtcpFeedback {

    /**
     * Temporal-Spatial Trade-off Request
     *
     *   The TSTR feedback message is identified by RTCP packet type value
     *    PT=PSFB and FMT=5.
     *
     *    The content of the FCI entry for the Temporal-Spatial Trade-off
     *    Request is depicted in Figure 5.  The length of the feedback message
     *    MUST be set to 2+2*N, where N is the number of FCI entries included.
     *    (Syntax of an FCI Entry in the TSTR Message)
     *
     *     0                   1                   2                   3
     *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *    |                              SSRC                             |
     *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *    |  Seq nr.      |  Reserved                           | Index   |
     *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     *      SSRC (32 bits): The SSRC of the media sender that is requested to
     *               apply the trade-off value given in Index.
     *
     *      Seq nr. (8 bits): Request sequence number.  The sequence number
     *               space is unique for pairing of the SSRC of request source
     *               and the SSRC of the request target.  The sequence number
     *               SHALL be increased by 1 modulo 256 for each new command.
     *               A repetition SHALL NOT increase the sequence number.  The
     *               initial value is arbitrary.
     *
     *      Reserved (19 bits): All bits SHALL be set to 0 by the sender and
     *               SHALL be ignored on reception.
     *
     *      Index (5 bits): An integer value between 0 and 31 that indicates
     *               the relative trade-off that is requested.  An index value
     *               of 0 indicates the highest possible spatial quality, while
     *               31 indicates the highest possible temporal resolution(high frame rate).
     *
     *
     *  A decoder can suggest a temporal-spatial trade-off level by sending a
     *    TSTR message to an encoder.
     *    > 디코더는 TST 레벨을 TSTR 메세지를 인코더에게 보냄으로써 정보를 보여줄 수 있다.
     *
     *  If the encoder is capable of adjusting
     *    its temporal-spatial trade-off, it SHOULD take into account the
     *    received TSTR message for future coding of pictures.
     *    > 인코더가 디코더가 원하는 TST 를 허용하면 추후에 사용될 프레임 코딩에
     *      인코더는 이 TSTR 메시지를 사용하면 된다.
     *
     */

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int MIN_LENGTH = RtcpFeedbackMessageHeader.LENGTH; // bytes


    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpTemporalSpatialTradeoffRequest(RtcpFeedbackMessageHeader rtcpFeedbackMessageHeader) {
        super(rtcpFeedbackMessageHeader);
    }

    public RtcpTemporalSpatialTradeoffRequest() {
    }

    public RtcpTemporalSpatialTradeoffRequest(byte[] data) {
        super(data);
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS


    ////////////////////////////////////////////////////////////

}
