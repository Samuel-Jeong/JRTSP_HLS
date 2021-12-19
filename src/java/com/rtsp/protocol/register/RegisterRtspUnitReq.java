package com.rtsp.protocol.register;

import com.rtsp.protocol.base.ByteUtil;
import com.rtsp.protocol.register.base.URtspHeader;
import com.rtsp.protocol.register.base.URtspMessageType;
import com.rtsp.protocol.register.exception.URtspException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

public class RegisterRtspUnitReq {

    private static final Logger log = LoggerFactory.getLogger(RegisterRtspUnitReq.class);

    private final URtspHeader uRtspHeader;

    private final int idLength;         // 4 bytes
    private final String id;            // idLength bytes
    private final long expires;         // 8 bytes
    private int nonceLength = 0;            // 4 bytes
    private String nonce = "";          // nonceLength bytes

    public RegisterRtspUnitReq(byte[] data) throws URtspException {
        if (data.length >= URtspHeader.U_RTSP_HEADER_SIZE + ByteUtil.NUM_BYTES_IN_LONG) {
            int index = 0;

            byte[] headerByteData = new byte[URtspHeader.U_RTSP_HEADER_SIZE];
            System.arraycopy(data, index, headerByteData, 0, headerByteData.length);
            this.uRtspHeader = new URtspHeader(headerByteData);
            index += headerByteData.length;

            byte[] idLengthByteData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, idLengthByteData, 0, idLengthByteData.length);
            idLength = ByteUtil.bytesToInt(idLengthByteData, true);
            index += idLengthByteData.length;

            byte[] idByteData = new byte[idLength];
            System.arraycopy(data, index, idByteData, 0, idByteData.length);
            id = new String(idByteData, StandardCharsets.UTF_8);
            index += idByteData.length;

            byte[] expiresByteData = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(data, index, expiresByteData, 0, expiresByteData.length);
            expires = ByteUtil.bytesToLong(expiresByteData, true);
            index += expiresByteData.length;

            byte[] nonceLengthByteData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, nonceLengthByteData, 0, nonceLengthByteData.length);
            nonceLength = ByteUtil.bytesToInt(nonceLengthByteData, true);
            if (nonceLength > 0) {
                index += nonceLengthByteData.length;

                byte[] nonceByteData = new byte[nonceLength];
                System.arraycopy(data, index, nonceByteData, 0, nonceByteData.length);
                nonce = new String(nonceByteData, StandardCharsets.UTF_8);
            }
        } else {
            this.uRtspHeader = null;
            this.idLength = 0;
            this.id = null;
            this.expires = 0;
        }
    }

    public RegisterRtspUnitReq(String magicCookie, URtspMessageType messageType, int seqNumber, long timeStamp, String id, long expires) {
        int bodyLength = id.length() + ByteUtil.NUM_BYTES_IN_LONG;

        this.uRtspHeader = new URtspHeader(magicCookie, messageType, seqNumber, timeStamp, bodyLength);
        this.expires = expires;
        this.idLength = id.length();
        this.id = id;
    }

    public byte[] getByteData() {
        byte[] data = new byte[URtspHeader.U_RTSP_HEADER_SIZE + ByteUtil.NUM_BYTES_IN_LONG + id.length() + nonceLength + ByteUtil.NUM_BYTES_IN_INT * 2];

        int index = 0;

        byte[] headerByteData = this.uRtspHeader.getByteData();
        System.arraycopy(headerByteData, 0, data, index, headerByteData.length);
        index += headerByteData.length;

        byte[] idLengthByteData = ByteUtil.intToBytes(idLength, true);
        System.arraycopy(idLengthByteData, 0, data, index, idLengthByteData.length);
        index += idLengthByteData.length;

        byte[] idByteData = id.getBytes(StandardCharsets.UTF_8);
        System.arraycopy(idByteData, 0, data, index, idByteData.length);
        index += idByteData.length;

        byte[] expiresByteData = ByteUtil.longToBytes(expires, true);
        System.arraycopy(expiresByteData, 0, data, index, expiresByteData.length);
        index += expiresByteData.length;

        byte[] nonceLengthByteData = ByteUtil.intToBytes(nonceLength, true);
        System.arraycopy(nonceLengthByteData, 0, data, index, nonceLengthByteData.length);
        index += nonceLengthByteData.length;

        if (nonceLength > 0 && nonce.length() > 0) {
            byte[] nonceByteData = nonce.getBytes(StandardCharsets.UTF_8);
            System.arraycopy(nonceByteData, 0, data, index, nonceByteData.length);
        }

        return data;
    }

    public URtspHeader getURtspHeader() {
        return uRtspHeader;
    }

    public long getExpires() {
        return expires;
    }

    public String getId() {
        return id;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonceLength = nonce.length();
        this.nonce = nonce;

        uRtspHeader.setBodyLength(id.length() + ByteUtil.NUM_BYTES_IN_LONG + nonceLength + ByteUtil.NUM_BYTES_IN_INT * 2);
    }
}
