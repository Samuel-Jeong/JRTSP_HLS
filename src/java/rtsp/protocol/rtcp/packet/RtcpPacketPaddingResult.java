package rtsp.protocol.rtcp.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class RtcpPacketPaddingResult {

    ////////////////////////////////////////////////////////////
    // VARIABLES
    private int length = 0;
    private int paddingBytes = 0;
    private boolean padding = false;
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpPacketPaddingResult(int length, int paddingBytes, boolean padding) {
        this.length = length;
        this.paddingBytes = paddingBytes;
        this.padding = padding;
    }

    public RtcpPacketPaddingResult() {}
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getPaddingBytes() {
        return paddingBytes;
    }

    public void setPaddingBytes(int paddingBytes) {
        this.paddingBytes = paddingBytes;
    }

    public boolean isPadding() {
        return padding;
    }

    public void setPadding(boolean padding) {
        this.padding = padding;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
