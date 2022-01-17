package rtsp.protocol.rtcp.type.extended.feedback;

import rtsp.protocol.rtcp.base.RtcpFormat;
import rtsp.protocol.rtcp.type.extended.feedback.base.RtcpFeedbackMessageHeader;

public class RtcpFeedback extends RtcpFormat {

    /**
     * @Reference https://datatracker.ietf.org/doc/html/rfc4585
     * @Reference https://datatracker.ietf.org/doc/html/rfc5104
     * @Reference https://datatracker.ietf.org/doc/html/rfc2032
     *
     *    AVPF:     Audio-Visual Profile with Feedback (RFC 4585)
     *    CCM:      Codec Control Message (RFC 5104)
     *    CNAME:    Canonical Name (RTCP Source Description)
     *    CSRC:     Contributing Source (RTP)
     *    FCI:      Feedback Control Information (AVPF)
     *    FIR:      Full Intra Refresh (CCM)
     *    FMT:      Feedback Message Type (AVPF)
     *    MCU:      Multipoint Control Unit
     *    MTU:      Maximum Transfer Unit
     *
     *    0                   1                   2                   3
     *    0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *   |V=2|P| FMT     |   PT          |          length               |
     *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-                                                                   +-+-+-+-+-+-+-+-+-+-+
     *   |                 SSRC of RTCP packet sender                    |
     *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *   |                   SSRC of 1st RTP Stream                      |
     *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *   |          begin_seq            |          num_reports          |
     *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *   |R|ECN|  Arrival time offset    | ...                           .
     *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *   .                                                               .
     *   .                                                               .
     *   .                                                               .
     *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *   |                   SSRC of nth RTP Stream                      |
     *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *   |          begin_seq            |          num_reports          |
     *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *   |R|ECN|  Arrival time offset    | ...                           |
     *   .                                                               .
     *   .                                                               .
     *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *   |                 Report Timestamp (32 bits)                    |
     *   +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     *   [SDP]
     *
     *       o "ccm", which indicates the support of codec control using RTCP feedback messages.
     *          The "ccm" feedback value SHOULD be used with parameters that indicate
     *          the specific codec control commands supported.
     *
     *       o  "fir" indicates support of the Full Intra Request (FIR).
     *       o  "tmmbr" indicates support of the Temporary Maximum Media Stream
     *          Bit Rate Request/Notification (TMMBR/TMMBN).  It has an
     *          optional sub-parameter to indicate the session maximum packet
     *          rate (measured in packets per second) to be used.  If not
     *          included, this defaults to infinity.
     *       o  "tstr" indicates support of the Temporal-Spatial Trade-off
     *          Request/Notification (TSTR/TSTN).
     *       o  "vbcm" indicates support of H.271 Video Back Channel Messages
     *          (VBCMs).  It has zero or more subparameters identifying the
     *          supported H.271 "payloadType" values.
     *
     *    rtcp-fb-val        =/ "ccm" rtcp-fb-ccm-param
     *
     *    rtcp-fb-ccm-param  = SP "fir"   ; Full Intra Request
     *                       / SP "tmmbr" [SP "smaxpr=" MaxPacketRateValue]
     *                                    ; Temporary max media bit rate
     *                       / SP "tstr"  ; Temporal-Spatial Trade-Off
     *                       / SP "vbcm" *(SP subMessageType) ; H.271 VBCMs
     *                       / SP token [SP byte-string]
     *                               ; for future commands/indications
     *    subMessageType = 1*8DIGIT
     *    byte-string = <as defined in section 4.2 of [RFC4585] >
     *    MaxPacketRateValue = 1*15DIGIT
     *
     *
     *   - Format : "a=rtcp-fb: " rtcp-fb-pt SP rtcp-fb-val CRLF
     *
     *          v=0
     *          o=alice 3203093520 3203093520 IN IP4 host.example.com
     *          s=Media with feedback
     *          t=0 0
     *          c=IN IP4 host.example.com
     *          m=audio 49170 RTP/AVPF 98
     *          a=rtpmap:98 H263-1998/90000
     *          a=rtcp-fb:98 nack pli
     *
     *      -------------> Offer
     *      v=0
     *      o=alice 3203093520 3203093520 IN IP4 host.example.com
     *      s=Offer/Answer
     *      c=IN IP4 192.0.2.124
     *      m=audio 49170 RTP/AVP 0
     *      a=rtpmap:0 PCMU/8000
     *      m=video 51372 RTP/AVPF 98
     *      a=rtpmap:98 H263-1998/90000
     *      a=rtcp-fb:98 ccm tstr
     *      a=rtcp-fb:98 ccm fir
     *      a=rtcp-fb:* ccm tmmbr smaxpr=120
     *
     *      The answerer wishes to support only the FIR and TSTR/TSTN messages
     *      and the answerer SDP is
     *
     *      <---------------- Answer
     *
     *      v=0
     *      o=alice 3203093520 3203093524 IN IP4 otherhost.example.com
     *      s=Offer/Answer
     *      c=IN IP4 192.0.2.37
     *      m=audio 47190 RTP/AVP 0
     *      a=rtpmap:0 PCMU/8000
     *      m=video 53273 RTP/AVPF 98
     *      a=rtpmap:98 H263-1998/90000
     *      a=rtcp-fb:98 ccm tstr
     *      a=rtcp-fb:98 ccm fir
     *
     */

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int MIN_LENGTH = RtcpFeedbackMessageHeader.LENGTH; // bytes

    private RtcpFeedbackMessageHeader rtcpFeedbackMessageHeader;
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpFeedback(RtcpFeedbackMessageHeader rtcpFeedbackMessageHeader) {
        this.rtcpFeedbackMessageHeader = rtcpFeedbackMessageHeader;
    }

    public RtcpFeedback() {}

    public RtcpFeedback(byte[] data) {}
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    public RtcpFeedbackMessageHeader getRtcpFeedbackMessageHeader() {
        return rtcpFeedbackMessageHeader;
    }

    public void setRtcpFeedbackMessageHeader(RtcpFeedbackMessageHeader rtcpFeedbackMessageHeader) {
        this.rtcpFeedbackMessageHeader = rtcpFeedbackMessageHeader;
    }
    ////////////////////////////////////////////////////////////

}
