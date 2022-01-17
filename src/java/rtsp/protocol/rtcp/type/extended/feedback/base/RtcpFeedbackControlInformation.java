package rtsp.protocol.rtcp.type.extended.feedback.base;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import rtsp.protocol.base.ByteUtil;

public class RtcpFeedbackControlInformation {

    /**
     * @Reference https://datatracker.ietf.org/doc/html/rfc7728
     *
     *  The FCI field consists of one or more PAUSE, RESUME, PAUSED, or
     *    REFUSED messages or any future extension.  These messages have the
     *    following FCI format:
     *    (Syntax of FCI Entry in the PAUSE and RESUME Message)
     *
     *       0                   1                   2                   3
     *       0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *      |                           Target SSRC                         |
     *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *      | Type  |  Res  | Parameter Len |           PauseID             |
     *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *      :                         Type Specific                         :
     *      +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     *
     *   The FCI fields have the following definitions:
     *
     *    Target SSRC (32 bits):  For a PAUSE-RESUME message, this value is the
     *       SSRC that the request is intended for.  For PAUSED, it MUST be the
     *       SSRC being paused.  If pausing is the result of a PAUSE request,
     *       the value in PAUSED is effectively the same as Target SSRC in a
     *       related PAUSE request.  For REFUSED, it MUST be the Target SSRC of
     *       the PAUSE or RESUME request that cannot change state.  A CSRC MUST
     *       NOT be used as a target as the interpretation of such a request is
     *       unclear.
     *
     *    Type (4 bits):  The pause feedback type.  The values defined in this
     *       specification are as follows:
     *
     *       0: PAUSE request message.
     *
     *       1: RESUME request message.
     *
     *       2: PAUSED indication message.
     *
     *       3: REFUSED notification message.
     *
     *      4-15:  Reserved for future use.  FCI fields with these Type values
     *          SHALL be ignored on reception by receivers and MUST NOT be used
     *          by senders implementing this specification.
     *
     *    Res: (4 bits):  Type Specific reserved.  It SHALL be ignored by
     *       receivers implementing this specification and MUST be set to 0 by
     *       senders implementing this specification.
     *
     *    Parameter Len (8 bits):  Length of the Type Specific field in 32-bit
     *       words.  MAY be 0.
     *
     *    PauseID (16 bits):  Message sequence identification, as described in
     *       Section 5.2.  SHALL be incremented by one modulo 2^16 for each new
     *       PAUSE message, unless the message is retransmitted.  The initial
     *       value SHOULD be 0.  The PauseID is scoped by the Target SSRC,
     *       meaning that PAUSE, RESUME, and PAUSED messages therefore share
     *       the same PauseID space for a specific Target SSRC.
     *
     *    Type Specific (variable):  Defined per pause feedback type.  MAY be
     *       empty.  A receiver implementing this specification MUST be able to
     *       skip and ignore any unknown Type Specific data, even for Type
     *       values defined in this specification.
     *
     *
     *  TMMBR/TMMBN MAY be used instead of the messages
     *  defined in this specification when the effective topology is point to point.
     *  > TMMBR 과 TMMBN 로직으로 위 메세지들을 대체할 수 있다.
     *
     *  This use is expected to be
     *    mainly for interworking with implementations that don't support the
     *    messages defined in this specification but make use of TMMBR/TMMBN to
     *    achieve a similar effect.
     *    > TMMBR/TMMBN 로직으로 사용해도 되고 이 방법도 사용해도 유사한 효과를 얻을 수 있다.
     *
     *  If either sender or receiver learns that
     *    the topology is not point to point, TMMBR/TMMBN MUST NOT be used for
     *    pause/resume functionality.
     *    > 단대단 네트워크 토폴로지가 아니면 TMMBR/TMMBN 로직을 pause/resume 로직으로 사용할 수 없다.
     *
     */

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int MIN_LENGTH = 8;
    private long targetSsrc = 0; // (32 bits)
    private byte type = 0; // (4 bits)
    private byte res = 0; // (4 bits)
    private short parameterLength = 0; // (8 bits)
    private int pauseId = 0; // (16 bits, sequential number by one, random initiation is needed)
    private byte[] typeSpecificData = null; // (variable)
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpFeedbackControlInformation(long targetSsrc, byte type, byte res, short parameterLength,
                                          int pauseId,
                                          byte[] typeSpecificData) {
        this.targetSsrc = targetSsrc;
        this.type = type;
        this.res = res;
        this.parameterLength = parameterLength;
        this.pauseId = pauseId;
        this.typeSpecificData = typeSpecificData;
    }

    public RtcpFeedbackControlInformation() {}

    public RtcpFeedbackControlInformation(byte[] data) {
        if (data.length >= MIN_LENGTH) {
            int index = 0;

            byte[] targetSsrcData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, targetSsrcData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] targetSsrcData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(targetSsrcData, 0, targetSsrcData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            targetSsrc = ByteUtil.bytesToLong(targetSsrcData2, true);
            index += ByteUtil.NUM_BYTES_IN_INT;

            byte[] typeResData = new byte[ByteUtil.NUM_BYTES_IN_BYTE]; // type + res
            System.arraycopy(data, index, typeResData, 0, ByteUtil.NUM_BYTES_IN_BYTE);
            type = (byte) (typeResData[0] >>> 0x04 & 0x05); // [0x04 + 0x01]? [for sign bit]?
            res = (byte) (typeResData[0] & 0x04);
            index += ByteUtil.NUM_BYTES_IN_BYTE;

            byte[] parameterLengthData = new byte[ByteUtil.NUM_BYTES_IN_BYTE];
            System.arraycopy(data, index, parameterLengthData, 0, ByteUtil.NUM_BYTES_IN_BYTE);
            parameterLength = parameterLengthData[0];
            index += ByteUtil.NUM_BYTES_IN_BYTE;

            byte[] pauseIdData = new byte[ByteUtil.NUM_BYTES_IN_SHORT];
            System.arraycopy(data, index, pauseIdData, 0, ByteUtil.NUM_BYTES_IN_SHORT);
            byte[] pauseIdData2 = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(pauseIdData, 0, pauseIdData2, ByteUtil.NUM_BYTES_IN_SHORT, ByteUtil.NUM_BYTES_IN_SHORT);
            pauseId = ByteUtil.bytesToInt(pauseIdData2, true);
            index += ByteUtil.NUM_BYTES_IN_INT;

            //int remainLength = data.length - index;
            //if (remainLength > 0) {
            if (parameterLength > 0) {
                //typeSpecificData = new byte[remainLength];
                typeSpecificData = new byte[parameterLength];
                System.arraycopy(data, index, typeSpecificData, 0, parameterLength);
            }
        }
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    public byte[] getData() {
        int index = 0;
        byte[] data = new byte[MIN_LENGTH];

        byte[] targetSsrcData = ByteUtil.longToBytes(targetSsrc, true);
        byte[] targetSsrcData2 = new byte[ByteUtil.NUM_BYTES_IN_INT];
        System.arraycopy(targetSsrcData, ByteUtil.NUM_BYTES_IN_INT, targetSsrcData2, 0, ByteUtil.NUM_BYTES_IN_INT);
        System.arraycopy(targetSsrcData2, 0, data, index, ByteUtil.NUM_BYTES_IN_INT);
        index += ByteUtil.NUM_BYTES_IN_INT;

        byte typeRes = 0;
        typeRes |= type;
        typeRes <<= 0x04;
        typeRes |= res;
        byte[] typeResData = { typeRes };
        System.arraycopy(typeResData, 0, data, index, ByteUtil.NUM_BYTES_IN_BYTE);
        index += ByteUtil.NUM_BYTES_IN_BYTE;

        byte[] parameterLengthData = ByteUtil.shortToBytes(parameterLength, true);
        byte[] parameterLengthData2 = new byte[ByteUtil.NUM_BYTES_IN_BYTE];
        System.arraycopy(parameterLengthData, ByteUtil.NUM_BYTES_IN_BYTE, parameterLengthData2, 0, ByteUtil.NUM_BYTES_IN_BYTE);
        System.arraycopy(parameterLengthData2, 0, data, index, ByteUtil.NUM_BYTES_IN_BYTE);
        index += ByteUtil.NUM_BYTES_IN_BYTE;

        byte[] pauseIdData = ByteUtil.intToBytes(pauseId, true);
        byte[] pauseIdData2 = new byte[ByteUtil.NUM_BYTES_IN_SHORT];
        System.arraycopy(pauseIdData, ByteUtil.NUM_BYTES_IN_SHORT, pauseIdData2, 0, ByteUtil.NUM_BYTES_IN_SHORT);
        System.arraycopy(pauseIdData2, 0, data, index, ByteUtil.NUM_BYTES_IN_SHORT);
        index += ByteUtil.NUM_BYTES_IN_SHORT;

        if (parameterLength > 0 && typeSpecificData != null && typeSpecificData.length > 0) {
            byte[] newData = new byte[MIN_LENGTH + parameterLength];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;

            System.arraycopy(typeSpecificData, 0, data, index, parameterLength);
        }

        return data;
    }

    public void setData(long targetSsrc, byte type, byte res, short parameterLength,
                        int pauseId,
                        byte[] typeSpecificData) {
        this.targetSsrc = targetSsrc;
        this.type = type;
        this.res = res;
        this.parameterLength = parameterLength;
        this.pauseId = pauseId;
        this.typeSpecificData = typeSpecificData;
    }

    public long getTargetSsrc() {
        return targetSsrc;
    }

    public void setTargetSsrc(long targetSsrc) {
        this.targetSsrc = targetSsrc;
    }

    public byte getType() {
        return type;
    }

    public void setType(byte type) {
        this.type = type;
    }

    public byte getRes() {
        return res;
    }

    public void setRes(byte res) {
        this.res = res;
    }

    public short getParameterLength() {
        return parameterLength;
    }

    public void setParameterLength(short parameterLength) {
        this.parameterLength = parameterLength;
    }

    public int getPauseId() {
        return pauseId;
    }

    public void setPauseId(int pauseId) {
        this.pauseId = pauseId;
    }

    public byte[] getTypeSpecificData() {
        return typeSpecificData;
    }

    public void setTypeSpecificData(byte[] typeSpecificData) {
        this.typeSpecificData = typeSpecificData;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
