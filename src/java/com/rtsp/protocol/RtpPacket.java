package com.rtsp.protocol;

import java.util.Arrays;
import java.util.Random;

/**
 * @class public class RtpPacket
 * @brief RtpPacket class
 */
public class RtpPacket {

    public static final int HEADER_SIZE = 12;

    public int version;
    public int padding;
    public int extension;
    public int csrc;
    public int marker;
    public int payloadType;
    public int sequenceNumber;
    public int timeStamp;
    public int ssrc;

    public byte[] header;
    public int payloadSize;
    public byte[] payload;

    ////////////////////////////////////////////////////////////////////////////////

    public RtpPacket(int payloadType, int frameIndex, int time, byte[] data, int dataLength){
        //fill by default header fields:
        version = 2;
        padding = 0;
        extension = 0;
        csrc = 0;
        marker = 0;

        Random random = new Random();
        ssrc = random.nextInt(Integer.MAX_VALUE);

        sequenceNumber = frameIndex;
        timeStamp = time;
        this.payloadType = payloadType;

        header = new byte[HEADER_SIZE];
        header[0] = (byte)(version << 6 | padding << 5 | extension << 4 | csrc);
        header[1] = (byte)(marker << 7 | this.payloadType & 0x000000FF);
        header[2] = (byte)(sequenceNumber >> 8);
        header[3] = (byte)(sequenceNumber & 0xFF);
        header[4] = (byte)(timeStamp >> 24);
        header[5] = (byte)(timeStamp >> 16);
        header[6] = (byte)(timeStamp >> 8);
        header[7] = (byte)(timeStamp & 0xFF);
        header[8] = (byte)(ssrc >> 24);
        header[9] = (byte)(ssrc >> 16);
        header[10] = (byte)(ssrc >> 8);
        header[11] = (byte)(ssrc & 0xFF);

        payloadSize = dataLength;
        payload = new byte[dataLength];
        payload = Arrays.copyOf(data, payloadSize);
    }

    public int getLength() {
        return payloadSize + HEADER_SIZE;
    }

    public void getPacket(byte[] packet) {
        //construct the packet = header + payload
        System.arraycopy(header, 0, packet, 0, HEADER_SIZE);
        if (payloadSize >= 0) {
            System.arraycopy(payload, 0, packet, 12, payloadSize);
        }
    }

    @Override
    public String toString() {
        return "[RTP-Header] Version: " + version
                           + ", Padding: " + padding
                           + ", Extension: " + extension
                           + ", CC: " + csrc
                           + ", Marker: " + marker
                           + ", PayloadType: " + payloadType
                           + ", SequenceNumber: " + sequenceNumber
                           + ", TimeStamp: " + timeStamp;
    }
}