package com.rtsp.protocol.register;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rtsp.protocol.base.ByteUtil;
import com.rtsp.protocol.register.base.URtspHeader;
import com.rtsp.protocol.register.base.URtspMessageType;
import com.rtsp.protocol.register.exception.URtspException;

import java.nio.charset.StandardCharsets;

public class RegisterRtspUnitReq {

    private static final Logger log = LoggerFactory.getLogger(RegisterRtspUnitReq.class);

    private final URtspHeader uRtspHeader;

    private final int idLength;         // 4 bytes
    private final String id;            // idLength bytes
    private final long expires;         // 8 bytes
    private final short listenPort;     // 2 bytes
    private int nonceLength = 0;            // 4 bytes
    private String nonce = "";          // nonceLength bytes

    public RegisterRtspUnitReq(byte[] data) throws URtspException {
        if (data.length >= URtspHeader.U_RTSP_HEADER_SIZE + ByteUtil.NUM_BYTES_IN_LONG + ByteUtil.NUM_BYTES_IN_SHORT) {
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

            byte[] listenPortByteData = new byte[ByteUtil.NUM_BYTES_IN_SHORT];
            System.arraycopy(data, index, listenPortByteData, 0, listenPortByteData.length);
            listenPort = ByteUtil.bytesToShort(listenPortByteData, true);
            index += listenPortByteData.length;

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
            this.listenPort = 0;
        }
    }

    public RegisterRtspUnitReq(String magicCookie, URtspMessageType messageType, int seqNumber, long timeStamp, String id, long expires, short listenPort) {
        int bodyLength = id.length() + ByteUtil.NUM_BYTES_IN_LONG + ByteUtil.NUM_BYTES_IN_INT * 2 + ByteUtil.NUM_BYTES_IN_SHORT;

        this.uRtspHeader = new URtspHeader(magicCookie, messageType, seqNumber, timeStamp, bodyLength);
        this.expires = expires;
        this.idLength = id.getBytes(StandardCharsets.UTF_8).length;
        this.id = id;
        this.listenPort = listenPort;
    }

    public byte[] getByteData() {
        byte[] data = new byte[URtspHeader.U_RTSP_HEADER_SIZE + this.uRtspHeader.getBodyLength()];
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

        byte[] listenPortByteData = ByteUtil.shortToBytes(listenPort, true);
        System.arraycopy(listenPortByteData, 0, data, index, listenPortByteData.length);
        index += listenPortByteData.length;

        byte[] nonceLengthByteData = ByteUtil.intToBytes(nonceLength, true);
        System.arraycopy(nonceLengthByteData, 0, data, index, nonceLengthByteData.length);

        if (nonceLength > 0 && nonce.length() > 0) {
            byte[] nonceByteData = nonce.getBytes(StandardCharsets.UTF_8);
            byte[] newData = new byte[data.length];
            System.arraycopy(data, 0, newData, 0, data.length);
            index += nonceLengthByteData.length;
            System.arraycopy(nonceByteData, 0, newData, index, nonceByteData.length);
            data = newData;
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

    public short getListenPort() {
        return listenPort;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonceLength = nonce.getBytes(StandardCharsets.UTF_8).length;
        this.nonce = nonce;

        uRtspHeader.setBodyLength(uRtspHeader.getBodyLength() + nonceLength);
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
}
