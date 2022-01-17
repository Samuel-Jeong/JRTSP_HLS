package rtsp.protocol.rtcp.type.extended.feedback.payloadspecific;

import rtsp.protocol.rtcp.type.extended.feedback.RtcpFeedback;
import rtsp.protocol.rtcp.type.extended.feedback.base.RtcpFeedbackMessageHeader;

public class RtcpPictureLossIndication extends RtcpFeedback {

    /**
     * Picture Loss Indication > 그냥 프레임 손실나고 있다고 디코더가 인코더한테 알려주기만 하는 트리거 메세지
     *
     *    The PLI FB message is identified by PT=PSFB and FMT=1.
     *
     *    There MUST be exactly one PLI contained in the FCI field.
     *
     *    With the Picture Loss Indication message, a decoder informs the
     *    encoder about the loss of an undefined amount of coded video data
     *    belonging to one or more pictures.
     *    > PLI 메세지는 디코더는 인코더에게 알 수 없는 양의 코딩된 비디오 데이터 손실이
     *      하나 또는 여러 프레임(미디어 조각)들에 발생하고 있다고 알려준다.
     *
     *    When used in conjunction with any video coding scheme that is based on inter-picture prediction,
     *    an encoder that receives a PLI becomes aware that the prediction chain may be broken.
     *    > 어떠한 내부 프레임 예측 방법에 기반한 비디오 코딩 스키마에 따른 비디오 합성이 사용되든지 간에,
     *      PLI 메세지를 수신한 인코더는 현재 프레임 예측 과정이 잘못되었다고 판단하게 된다.
     *
     *    The sender MAY react to a PLI by transmitting an
     *    intra-picture to achieve resynchronization (making this message
     *    effectively similar to the FIR message as defined in [6])
     *    > PLI 메세지 송신자는 비디오 재동기화를 위해 내부 프레임 데이터를 전송한다.
     *
     *    however,
     *    the sender MUST consider congestion control as outlined in Section 7,
     *    which MAY restrict its ability to send an intra frame.
     *    > 하지만 PLI 메세지 송신자는 intra-frame 전송과 관련해서는 혼잡 제어에 대해 반드시 고려해야 한다.
     *      (inter-frame 는 고려 대상 아님)
     *
     *    PLI messages typically trigger the sending of full intra-pictures.
     *    Intra-pictures are several times larger then predicted (inter-)
     *    pictures.  Their size is independent of the time they are generated.
     *    In most environments, especially when employing bandwidth-limited
     *    links, the use of an intra-picture implies an allowed delay that is a
     *    significant multitude of the typical frame duration.
     *    > PLI 메세지는 대체적으로 full intra-frame 에 전송을 트리거한다.
     *    > Intra-frame 들은 예측된 inter-frame 들보다 더 클 수도 있는 경우도 여러 번 있다. (자주? or 가끔?)
     *      이 intra-frame 의 크기는 생성된 시간에 상관없다.
     *    > 대부분의 네트워크 환경에서 특히 intra-frame 을 사용할 때 전송 딜레이는 프레임 전송 딜레이의 배수로 적용된다. (당연한 말 아님?)
     *
     */

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int MIN_LENGTH = RtcpFeedbackMessageHeader.LENGTH; // bytes

    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpPictureLossIndication(RtcpFeedbackMessageHeader rtcpFeedbackMessageHeader) {
        super(rtcpFeedbackMessageHeader);
    }

    public RtcpPictureLossIndication() {
    }

    public RtcpPictureLossIndication(byte[] data) {
        super(data);

        if (data.length >= MIN_LENGTH) {
            int index = 0;

            byte[] headerData = new byte[RtcpFeedbackMessageHeader.LENGTH];
            System.arraycopy(data, index, headerData, 0, RtcpFeedbackMessageHeader.LENGTH);
            setRtcpFeedbackMessageHeader(new RtcpFeedbackMessageHeader(headerData));
        }
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    @Override
    public byte[] getData() {
        if (getRtcpFeedbackMessageHeader() == null) { return null; }

        int index = 0;
        byte[] data = new byte[MIN_LENGTH];

        byte[] headerData = getRtcpFeedbackMessageHeader().getData();
        System.arraycopy(headerData, 0, data, index, headerData.length);
        //index += headerData.length;

        return data;
    }

    ////////////////////////////////////////////////////////////

}
