package rtsp.protocol.rtcp.packet;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class RtcpCompoundPacket {

    private static final Logger logger = LoggerFactory.getLogger(RtcpCompoundPacket.class);

    ////////////////////////////////////////////////////////////
    // VARIABLES
    private List<RtcpPacket> rtcpPacketList;
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpCompoundPacket(List<RtcpPacket> rtcpPacketList) {
        this.rtcpPacketList = rtcpPacketList;
    }

    public RtcpCompoundPacket() {}
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    public byte[] getData() {
        int totalLength = getTotalRtcpPacketSize();
        if (totalLength <= 0) {
            return null;
        }

        byte[] data = new byte[totalLength];
        int index = 0;

        for (RtcpPacket rtcpPacket : rtcpPacketList) {
            if (rtcpPacket == null) { continue; }

            byte[] curPacketData = rtcpPacket.getData();
            if (curPacketData == null) { continue; }

            System.arraycopy(curPacketData, 0, data, index, curPacketData.length);
            index += curPacketData.length;
        }

        return data;
    }

    public void setData(List<RtcpPacket> rtcpPacketList) {
        this.rtcpPacketList = rtcpPacketList;
    }

    public void addRtcpPacketToList(RtcpPacket rtcpPacket) {
        if (rtcpPacket == null) { return; }
        if (rtcpPacketList == null) {
            rtcpPacketList = new ArrayList<>();
        } else {
            if (rtcpPacketList.contains(rtcpPacket)) { return; }
        }

        rtcpPacketList.add(rtcpPacket);
    }

    public void addRtcpPacketToListAt(int index, RtcpPacket rtcpPacket) {
        if (rtcpPacket == null || index < 0) { return; }
        if (rtcpPacketList == null) {
            rtcpPacketList = new ArrayList<>();
        } else {
            if (rtcpPacketList.contains(rtcpPacket)) { return; }
        }

        rtcpPacketList.add(index, rtcpPacket);
    }

    public void removeRtcpPacketFromListByObject(RtcpPacket rtcpPacket) {
        if (rtcpPacketList == null || rtcpPacket == null) { return; }

        rtcpPacketList.remove(rtcpPacket);
    }

    public void removeRtcpPacketFromListByIndex(int index) {
        if (rtcpPacketList == null || index < 0 || index >= rtcpPacketList.size()) { return; }

        rtcpPacketList.remove(index);
    }

    public List<RtcpPacket> getRtcpPacketList() {
        return rtcpPacketList;
    }

    public int getTotalRtcpPacketSize() {
        if (rtcpPacketList == null || rtcpPacketList.isEmpty()) { return 0; }

        int totalSize = 0;
        for (RtcpPacket rtcpPacket : rtcpPacketList) {
            if (rtcpPacket == null) { continue; }
            totalSize += rtcpPacket.getData().length;
        }

        return totalSize;
    }

    public RtcpPacket getRtcpPacketByIndex(int index) {
        if (rtcpPacketList == null || index < 0 || index >= rtcpPacketList.size()) { return null; }

        return rtcpPacketList.get(index);
    }

    public void setRtcpPacketList(List<RtcpPacket> rtcpPacketList) {
        this.rtcpPacketList = rtcpPacketList;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
