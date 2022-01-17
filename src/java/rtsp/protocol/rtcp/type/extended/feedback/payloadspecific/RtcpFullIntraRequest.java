package rtsp.protocol.rtcp.type.extended.feedback.payloadspecific;

import rtsp.protocol.rtcp.type.extended.feedback.RtcpFeedback;
import rtsp.protocol.rtcp.type.extended.feedback.base.RtcpFeedbackMessageHeader;

public class RtcpFullIntraRequest extends RtcpFeedback { // Full INTRA-frame Request

    /**
     * FIR is also known as an "instantaneous decoder refresh request",
     *    "fast video update request" or "video fast update request".
     *
     *      0                   1                   2                   3
     *      0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *     |V=2|P|   MBZ   |  PT=RTCP_FIR  |           length              |
     *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *     |                              SSRC                             |
     *     +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     *
     *    Long-standing experience of the conversational video
     *    conferencing industry suggests that there is a need for a few
     *    additional feedback messages, to support centralized multipoint
     *    conferencing efficiently.
     *
     *    Some of the messages have applications
     *    beyond centralized multipoint, and this is indicated in the
     *    description of the message.
     *
     *    A Full Intra Request (FIR) Command, when received by the designated
     *    media sender, requires that the media sender sends a Decoder Refresh
     *    Point (see section 2.2) at the earliest opportunity.
     *    The evaluation of such an opportunity includes the current encoder coding strategy
     *    and the current available network resources.
     *
     *    The purpose is to speed up refreshment of the
     *    video in those situations where their use is feasible.
     *
     *    If using immediate feedback mode,
     *    the repetition SHOULD wait at least one RTT before being sent.
     *    In early or regular RTCP mode, the repetition is sent in the next regular RTCP packet.
     *    > 일반적으로 FIR 패킷은 다음 RTCP 패킷 전송 주기에 따라 재전송된다.
     *
     *    In conjunction with video codecs, FIR messages typically trigger the
     *    sending of full intra or IDR pictures.  Both are several times larger
     *    than predicted (inter) pictures.  Their size is independent of the
     *    time they are generated.
     *    > FIR 메세지는 해당 메세지 수신단으로 하여금 i-frame 과 idr 조각들을 보내도록 트리거한다.
     *      해당 프레임 또는 조각들의 크기는 기존 조각들보다 더 크지만, 생성된 시간에 비례하지 않는다.
     *
     *  if the sending frame rate is 10 fps,
     *      and an intra picture is assumed to be 10 times as big as an inter picture,
     *      then a full second of latency has to be accepted.
     *      >> Intra-frame means that all the compression is done within that
     *          single frame and generates what is sometimes referred to as an i-frame.
     *      >> Inter-frame refers to compression that
     *          takes place across two or more frames,
     *          where the encoding scheme only keeps the information that changes between frames.
     *      > 프레임 전송 비율이 10 fps 이고, intra-frame 이 inter-frame 보다 10 배 더 크기가 크다고 하면,
     *          full second of latency(?) 가 승인된다.
     *
     *    Mandating a maximum delay for completing the sending of a decoder
     *    refresh point would be desirable from an application viewpoint, but
     *    is problematic from a congestion control point of view.  "As soon as
     *    possible" as mentioned above appears to be a reasonable compromise.
     *    > 어플리케이션 입장에서는 최대한 긴 decoder refresh point 를 전달받아야 한다.
     *      하지만 네트워크 혼잡도에 문제가 생긴다.
     *
     *  In environments where the sender has no control over the codec (e.g.,
     *    when streaming pre-recorded and pre-coded content), the reaction to
     *    this command cannot be specified.  One suitable reaction of a sender
     *    would be to skip forward in the video bit stream to the next decoder
     *    refresh point.
     *    > 미디어 송신단에서 특정 코덱(미디어)에 대한 혼잡 제어를 하지 않으면, FIR 에 대해 반응하지 않는다.
     *      그래서 그냥 이런 상황에서는 다음 decoder refresh point 까지 비디오 비트 스트림을 스킵해버린다.
     *
     *  However, a session that predominantly handles pre-coded
     *    content is not expected to use FIR at all.
     *    > 사전에 미리 코딩된 미디어 데이터를 다루는 세션에는 FIR 사용이 필수는 아니다.
     *
     *
     *  Picture Loss Indication informs the decoder about the loss of a picture and
     *    hence the likelihood of misalignment of the reference pictures
     *    between the encoder and decoder.
     *    > PLI 패킷은 디코더로 하여금
     *      프레임 손실과 이에 따른 인코더와 디코더 사이의 프레임 동기화가 불일치할 가능성에 대해 알려준다.
     *
     */

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int MIN_LENGTH = RtcpFeedbackMessageHeader.LENGTH; // bytes


    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpFullIntraRequest(RtcpFeedbackMessageHeader rtcpFeedbackMessageHeader) {
        super(rtcpFeedbackMessageHeader);
    }

    public RtcpFullIntraRequest() {
    }

    public RtcpFullIntraRequest(byte[] data) {
        super(data);
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS


    ////////////////////////////////////////////////////////////

}
