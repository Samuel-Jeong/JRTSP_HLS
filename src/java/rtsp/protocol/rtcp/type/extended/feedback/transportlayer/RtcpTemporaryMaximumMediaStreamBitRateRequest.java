package rtsp.protocol.rtcp.type.extended.feedback.transportlayer;

import rtsp.protocol.rtcp.type.extended.feedback.RtcpFeedback;
import rtsp.protocol.rtcp.type.extended.feedback.base.RtcpFeedbackMessageHeader;

public class RtcpTemporaryMaximumMediaStreamBitRateRequest extends RtcpFeedback {

    /**
     * @Reference https://datatracker.ietf.org/doc/html/rfc5104#page-29
     */

    /**
     * @ Semantics
     *
     * @ 사전 개념 정의
     * - 미디어 송신단, 수신단 모두 하나의 세션에서의 각각 하나의 참가자이다. (멀티미디어 환경)
     *
     * @ 수신단 입장에서의 로직 정의
     * Behaviour at the Media Receiver (Sender of the TMMBR)
     *
     * TMMBR is used to indicate a transport-related limitation at the
     *    reporting entity acting as a media receiver.
     *
     *    TMMBR has the form of a tuple containing two components.
     *    1) The first value is the highest bit rate per sender of a media stream,
     *          available at a receiver-chosen protocol layer,
     *          which the receiver currently supports in this RTP session.
     *          > 첫 번째 값은 미디어 스트림 송신부들과 협상된 최대 비트레이트
     *    2) The second value is the measured header overhead in bytes
     *          as defined in section 2.2 and measured at the chosen protocol layer
     *          in the packets received for the stream.
     *          > 두 번째 값은 미디어 스트림에서 받은 패킷들의 오버헤드 바이트 수
     *    > 이 두 값을 묶어서 "tuple" 이라고 부름
     *
     *  The measurement of the
     *    overhead is a running average that is updated for each packet
     *    received for this particular media source (SSRC), using the following
     *    formula:
     *
     *        avg_OH (new) = 15/16*avg_OH (old) + 1/16*pckt_OH,
     *
     *    where avg_OH is the running (exponentially smoothed) average and
     *    pckt_OH is the overhead observed in the latest packet.
     *    > avg_OH : 미디어 스트림에서 패킷들의 평균 오버헤드 바이트 수
     *    > pckt_OH : 가장 마지막으로(최근) 수신한 패킷의 오버헤드 바이트 수
     *
     *    If a maximum bit rate has been negotiated through signaling, the
     *    maximum total media bit rate that the receiver reports in a TMMBR
     *    message MUST NOT exceed the negotiated value converted to a common
     *    basis (i.e., with overheads adjusted to bring it to the same
     *    reference protocol layer).
     *    > 만약에 시그널 단(레벨)에서 이미 최대 비트레이트를 정했다면,
     *      TMMBR 메시지에서는 해당 비트레이트를 초과하여 설정될 수 없다.
     *
     *
     *    Within the common packet header for feedback messages (as defined in
     *    section 6.1 of [RFC4585]), the "SSRC of packet sender" field
     *    indicates the source of the request, and the "SSRC of media source"
     *    is not used and SHALL be set to 0.  Within a particular TMMBR FCI
     *    entry, the "SSRC of media source" in the FCI field denotes the media
     *    sender that the tuple applies to.  This is useful in the multicast or
     *    translator topologies where the reporting entity may address all of
     *    the media senders in a single TMMBR message using multiple FCI
     *    entries.
     *    > SSRC 가 명시되어 있지 않으면 여러 개의 FCI 엔트리 정보를 담은 TMMBR 메시지를
     *      미디어(RTP) 송신단에 멀티 캐스팅 가능하다.
     *
     *  The media receiver SHALL save the contents of the latest TMMBN
     *    message received from each media sender.
     *    > 미디어 수신단에서 가장 마지막으로 수신한 TMMBN 메시지의 내용을 저장 가능
     *
     *  The media receiver MAY send a TMMBR FCI entry to a particular media
     *    sender under the following circumstances:
     *    > 미디어 수신단에서 특정 미디어 송신단으로 하나의 TMMBR FCI 를 전송 가능
     *
     *      o   before any TMMBN message has been received from that media
     *          sender;
     *          > 아직 미디어 송신단으로부터 TMMBN 을 하나도 못 받은 경우 요청 가능
     *
     *      o   when the media receiver has been identified as the source of a
     *          bounding tuple within the latest TMMBN message received from
     *          that media sender, and the value of the maximum total media bit
     *          rate or the overhead relating to that media sender has changed;
     *          > 미디어 수신단이 가장 최근에 수신한 TMMBN 메시지의 송신단과 바운딩하고 있는
     *          튜플의 최대 비트 레이트 또는 오버헤드 값이 변경된 경우 요청 가능
     *
     *      o   when the media receiver has not been identified as the source
     *          of a bounding tuple within the latest TMMBN message received
     *          from that media sender, and, after the media receiver applies
     *          the incremental algorithm from section 3.5.4.2 or a stricter
     *          equivalent, the media receiver's tuple relating to that media
     *          sender is determined to belong to the bounding set.
     *          > 미디어 수신단이 가장 최근에 수신한 TMMBN 메시지의 송신단과 바운딩하고 있는 튜플이 없으면,
     *          해당 메시지의 송신단이 가지고 있는 바운딩 셋 중 하나로 튜플을 결정해야할 경우 요청 가능
     *
     *   A TMMBR FCI entry MAY be repeated in subsequent TMMBR messages if no
     *    Temporary Maximum Media Stream Bit Rate Notification (TMMBN) FCI has
     *    been received from the media sender at the time of transmission of
     *    the next RTCP packet.  The bit rate value of a TMMBR FCI entry MAY be
     *    changed from one TMMBR message to the next.  The overhead measurement
     *    SHALL be updated to the current value of avg_OH each time the entry
     *    is sent.
     *    > 미디어 수신단에서 TMMBR FCI 메시지를 보냈지만, 미디어 송신단에서 보낸 TMMBN 메시지를 수신하지 못하면, 재전송 가능하다.
     *      비트레이트 값은 다음 TMMBR 메시지의 FCI 엔트리에 정의된 값으로 변경된다.
     *      TMMBR 메시지를 보낼 때마다 avg_OH 값을 계속 갱신한다.
     *
     *  If the value set by a TMMBR message is expected to be permanent, the
     *    TMMBR setting party SHOULD renegotiate the session parameters to
     *    reflect that using session setup signaling, e.g., a SIP re-invite.
     *    > 만약 TMMBR 메시지에 의해 튜플값이 영구적으로 고정되고 값을 업데이트하려면, SIP re-invite 와 같은 시그널링 단 메시지로 다시 협상해야 한다.
     *
     *
     * @ 송신단 입장에서의 로직 정의
     * Behaviour at the Media Sender (Receiver of the TMMBR)
     *
     *  When it receives a TMMBR message containing an FCI entry relating to
     *    it, the media sender SHALL use an initial or incremental algorithm as
     *    applicable to determine the bounding set of tuples based on the new
     *    information.  The algorithm used SHALL be at least as strict as the
     *    corresponding algorithm defined in section 3.5.4.2.  The media sender
     *    MAY accumulate TMMBRs over a small interval (relative to the RTCP
     *    sending interval) before making this calculation.
     *    > 미디어 송신단에서 FCI 가 포함된 TMMBR 메세지를 받으면,
     *    "the bounding set of tuples based on the new information" (미디어 스트림 최대 비트레이트 정보) 을 생성(결정)한다.
     *
     *  Once it has determined the bounding set of tuples, the media sender
     *    MAY use any combination of packet rate and net media bit rate within
     *    the feasible region that these tuples describe to produce a lower
     *    total media stream bit rate, as it may need to address a congestion
     *    situation or other limiting factors.  See section 5 (congestion
     *    control) for more discussion.
     *    > 튜플이 결정되면 미디어 송신단은 튜플에서 결정된 비트레이트 보다 낮은 비트레이트로 미디어를 송출한다.
     *      >> 미디어 혼잡 상황이나 기타 다른 제한된 요인들을 고려하기 위해서이다.
     *
     *  If the media sender concludes that it can increase the maximum total
     *    media bit rate value, it SHALL wait before actually doing so, for a
     *    period long enough to allow a media receiver to respond to the TMMBN
     *    if it determines that its tuple belongs in the bounding set.  This
     *    delay period is estimated by the formula:
     *    > 만약 미디어 송신단에서 최대 비트레이트 값을 올리기로 결정하면,
     *      아래 정의된 공식으로 산출된 기간동안 미디어 수신단에서 보내는 TMMBR 에 응답하기 위한 TMMBN 에
     *      변경된 튜플 정보를 정의해서 응답할 수 있다.
     *
     *       2 * RTT + T_Dither_Max,
     *              > T_dither_max : The maximum interval for which an RTCP feedback packet.
     *                  Dynamically calculated based upon T_rr. (T_rr : Regular RTCP interval)
     *                  (or may be derived by means of another mechanism common across all RTP receivers to be specified in the future).
     *                  For point-to-point sessions, (i.e., sessions with exactly two members with no change in the group size expected, e.g., unicast streaming sessions)
     *                  T_dither_max is set to 0.
     *
     *
     *    where RTT is the longest round trip time known to the media sender
     *    and T_Dither_Max is defined in section 3.4 of [RFC4585].
     *    Even in point-to-point sessions, a media sender MUST obey the aforementioned rule,
     *    as it is not guaranteed that a participant is able to determine correctly
     *    whether all the sources are co-located in a single node, and are coordinated.
     *    > RTT 는 가장 긴 왕복 시간을 의미한다.
     *      엔드포인트 간 세션일지라도, 미디어 송신단은 앞서 언급한 방식대로 동작한다.
     *      모든 미디어 소스들의 위치가 같은지 다른지에 대한 여부를 정확하게 결정할 수 있는 참여자가 보장되지 않은 경우도 동일하게 동작한다.
     *
     *
     *  An SSRC may time out according to the default rules for RTP session
     *    participants, i.e., the media sender has not received any RTP or RTCP
     *    packets from the owner for the last five regular reporting intervals.
     *    > SSRC 는 미디어 송신단에서 어떤 RTP 또는 RTCP 패킷을 미디어 발생지(owner, origin)로부터
     *      특정 간격에 맞춰 5개 이상 못받으면 타임 아웃 처리하여 해당 정보를 세션에서 삭제한다.
     *  An SSRC may also explicitly leave the session, with the participant
     *    indicating this through the transmission of an RTCP BYE packet or
     *    using an external signaling channel.
     *    > 또한, RTCP BYE 또는 외부 시그널링(SIP bye)에 의해서 세션에서 삭제되기도 한다.
     *
     *  If the media sender determines
     *    that the owner of a tuple in the bounding set has left the session,
     *    the media sender SHALL transmit a new TMMBN containing the previously
     *    determined set of bounding tuples but with the tuple belonging to the
     *    departed owner removed.
     *    > 만약 미디어 송신단에서 해당 미디어에 대한 튜플을 세션에서 삭제하려고 한다면,
     *      바로 이전에 TMMBN 에 담아서 보낸 튜플을 다시 미디어 수신단으로 보낸다.
     *      (해당 튜플 정보는 미디어에서 삭제된 정보이어야 한다.)
     *
     *
     *  A media sender MAY proactively initiate the equivalent to a TMMBR
     *    message to itself, when it is aware that its transmission path is
     *    more restrictive than the current limitations.  As a result, a TMMBN
     *    indicating the media source itself as the owner of a tuple is being
     *    sent, thereby avoiding unnecessary TMMBR messages from other
     *    participants.  However, like any other participant, when the media
     *    sender becomes aware of changed limitations, it is required to change
     *    the tuple, and to send a corresponding TMMBN.
     *    > 미디어 송신단에서는 현재 미디어 제한 정보보다 실제 전송 경로가 더 제한적이면, 사전에 스스로 TMMBR 메시지를 생성해둔다.
     *      그래서 미디어 송신단에서 자체적으로 불필요한 TMMBR 메시지를 피할 수 있게 된다. (avoid or ignore)
     *    > 하지만, 미디어 제한 정보(튜플)를 변경해야할 때는 이에 대응하는 TMMBN 메시지를 응답해야 한다.
     *
     *
     * @ Discussion
     *    Due to the unreliable nature of transport of TMMBR and TMMBN, the
     *    above rules may lead to the sending of TMMBR messages that appear to
     *    disobey those rules.  Furthermore, in multicast scenarios it can
     *    happen that more than one "non-owning" session participant may
     *    determine, rightly or wrongly, that its tuple belongs in the bounding
     *    set.  This is not critical for a number of reasons:
     *    > TMMBR 과 TMMBN 은 비신뢰성 메시지이므로 위에 설명한 규칙들이 제대로 지켜질지 모른다.
     *      특히 멀티캐스팅 시나리오(환경)에서는 어떤 참가자도 소유하지 않은 (특정 미디어 튜플에 바운딩된) 세션이
     *      잘못 혹은 정상적으로 하나 이상 생성될지도 모른다.
     *      하지만 아래와 같은 경우들에서는 크리티컬하지 않다.
     *
     *   a) If a TMMBR message is lost in transmission, either the media
     *       sender sends a new TMMBN message in response to some other media
     *       receiver or it does not send a new TMMBN message at all.  In the
     *       first case, the media receiver applies the incremental algorithm
     *       and, if it determines that its tuple should be part of the
     *       bounding set, sends out another TMMBR.  In the second case, it
     *       repeats the sending of a TMMBR unconditionally.  Either way, the
     *       media sender eventually gets the information it needs.
     *       > 만약 TMMBR 메세지가 전송 간 유실되는 경우,
     *          또는 미디어 송신단에서 TMMBR 을 수신했지만 다른 미디어 수신단으로 TMMBN 메세지를 보낸 경우,
     *          또는 아예 미디어 수신단으로 TMMBN 메세지를 안보낸 경우
     *
     *    b) Similarly, if a TMMBN message gets lost, the media receiver that
     *       has sent the corresponding TMMBR does not receive the notification
     *       and is expected to re-send the request and trigger the
     *       transmission of another TMMBN.
     *       > TMMBN 메시지가 어떤 경우에서든 유실되었을 때, 미디어 수신단에서 TMMBR 메세지를 재전송한 경우
     *
     *    c) If multiple competing TMMBR messages are sent by different session
     *       participants, then the algorithm can be applied taking all of
     *       these messages into account, and the resulting TMMBN provides the
     *       participants with an updated view of how their tuples compare with
     *       the bounded set.
     *       > 만약 다른 세션 참가자들에게 같은 TMMBR 메세지를 여러 번 보낼 경우,
     *         이에 대한 답으로 전달 받은 TMMBN 에 있는 튜플 정보가 세션 내 모든 참가자들에게 제공되는 경우
     *
     *    d) If more than one session participant happens to send TMMBR
     *       messages at the same time and with the same tuple component
     *       values, it does not matter which of those tuples is taken into the
     *       bounding set.  The losing session participant will determine,
     *       after applying the algorithm, that its tuple does not enter the
     *       bounding set, and will therefore stop sending its TMMBR.
     *       > 동시에 하나 이상의 세션에서 같은 튜플 정보를 담은 TMMBR 메세지를 동시에 보내게 되더라도,
     *         그 튜플들 중 어느 하나가 바운딩 셋에 포함되어 있는지는 중요하지 않다.
     *       > 어떤 참가자가 세션을 잃어버리면, 그 참가자가 가지고 있던 미디어 튜플은 바운딩 셋에 포함될 수 없고,
     *         해당 참가자는 더 이상 TMMBR 을 다른 참가자들한테 보내지 못하는 경우
     *
     *  It is important to consider the security risks involved with faked
     *    TMMBRs.  See the security considerations in section 6.
     *    > 가짜 TMMBR 메세지들에 대해 보안 위협을 고려하기 위해 중요한 사항들이다.
     *
     *   As indicated already, the feedback messages may be used in both
     *    multicast and unicast sessions in any of the specified topologies.
     *    However, for sessions with a large number of participants, using the
     *    lowest common denominator, as required by this mechanism, may not be
     *    the most suitable course of action.  Large sessions may need to
     *    consider other ways to adapt the bit rate to participants' capabilities,
     *    such as partitioning the session into different quality tiers
     *    or using some other method of achieving bit rate scalability.
     *    > 피드백 메세지들은 멀티캐스팅이든 유니캐스팅이든 상관없이 특정 네트워크 토폴로지에서 사용될 수 있다.
     *      하지만, 아주 많은 수의 참가자들이 포함된 세션들에는 위에 기술된 방법들이 가장 적절하지 않을 수도 있다.
     *    > 많은 수의 세션들은 각 세션의 참가자들의 수용 능력에 따라 비트 레이트를 조정하는 길을 따로 정할 필요가 있다.
     *      예를 들어, 다른 티어 품질의 세션에 참가시킬 수 있고, 확장 가능한 비트 레이트를 여러 방법을 통해 적용시킬 수 있다.
     *
     * @ Media Translators and Mixers
     * - Translator
     *      > TMMBR 또는 TMMBN 을 포워딩만 한다.
     * - Mixers
     *      > TMMBR 또는 TMMBN 을 생성하거나 포워딩한다.
     *      > TMMBN 을 원래 요청한 곳으로 보낼 필요가 있다.
     *      (규격 문서에 will need 라고 되어 있음, 보내면 좋은데 굳이 안보내도 됨, to indicate that it is handling the request.)
     *
     */

    /**
     * The Feedback Control Information (FCI) consists of one or more TMMBR
     *    FCI entries with the following syntax:
     *    (Syntax of an FCI Entry in the TMMBR Message)
     *
     *     0                   1                   2                   3
     *     0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *    |                              SSRC                             |
     *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *    | MxTBR Exp |  MxTBR Mantissa                 |Measured Overhead|
     *    +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     *      SSRC (32 bits): The SSRC value of the media sender that is
     *               requested to obey the new maximum bit rate.
     *
     *      MxTBR Exp (6 bits): The exponential scaling of the mantissa for the
     *               maximum total media bit rate value.  The value is an
     *               unsigned integer [0..63].
     *               > 최대 비트레이트 지수(exponential) (밑이 2)
     *
     *      MxTBR Mantissa (17 bits): The mantissa of the maximum total media
     *               bit rate value as an unsigned integer.
     *               > 최대 비트레이트 가수(mantissa)
     *
     *      Measured Overhead (9 bits): The measured average packet overhead
     *               value in bytes.  The measurement SHALL be done according
     *               to the description in section 4.2.1.2. The value is an
     *               unsigned integer [0..511].
     *
     *   The maximum total media bit rate (MxTBR) value in bits per second is
     *    calculated from the MxTBR exponent (exp) and mantissa in the
     *    following way:
     *
     *       MxTBR = mantissa * 2^exp
     *
     *    This allows for 17 bits of resolution in the range 0 to 131072*2^63
     *    (approximately 1.2*10^24).
     *
     *    The length of the TMMBR feedback message SHALL be set to 2+2*N where
     *    N is the number of TMMBR FCI entries.
     *    > TMMBN 메세지 길이는 2 + 2*N 로 정의되고 N 은 TMMBN 내에 정의된 FCI 엔트리 개수이다.
     *
     */

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int MIN_LENGTH = RtcpFeedbackMessageHeader.LENGTH; // bytes


    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpTemporaryMaximumMediaStreamBitRateRequest(RtcpFeedbackMessageHeader rtcpFeedbackMessageHeader) {
        super(rtcpFeedbackMessageHeader);
    }

    public RtcpTemporaryMaximumMediaStreamBitRateRequest() {
    }

    public RtcpTemporaryMaximumMediaStreamBitRateRequest(byte[] data) {
        super(data);
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS


    ////////////////////////////////////////////////////////////

}
