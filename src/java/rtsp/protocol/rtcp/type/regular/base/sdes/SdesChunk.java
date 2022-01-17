package rtsp.protocol.rtcp.type.regular.base.sdes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import rtsp.protocol.base.ByteUtil;
import rtsp.protocol.rtcp.packet.RtcpPacket;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SdesChunk {

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int MIN_LENGTH = 4; // bytes

    // SSRC
    // The synchronization source identifier for the originator of this SR packet.
    private long ssrc = 0; // (32 bits)

    // SdesItem List
    private List<SdesItem> sdesItemList = null;
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public SdesChunk(long ssrc, List<SdesItem> sdesItemList) {
        this.ssrc = (int) ssrc;
        this.sdesItemList = sdesItemList;
    }

    public SdesChunk() {}

    public SdesChunk(byte[] data) {
        if (data.length >= MIN_LENGTH) {
            int index = 0;

            // SSRC
            byte[] ssrcData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, ssrcData, 0, ByteUtil.NUM_BYTES_IN_INT);
            byte[] ssrcData2 = new byte[ByteUtil.NUM_BYTES_IN_LONG];
            System.arraycopy(ssrcData, 0, ssrcData2, ByteUtil.NUM_BYTES_IN_INT, ByteUtil.NUM_BYTES_IN_INT);
            ssrc = ByteUtil.bytesToLong(ssrcData2, true);
            index += ByteUtil.NUM_BYTES_IN_INT;

            // SdesItem List
            // The list of items in each chunk is
            //   terminated by one or more null octets, the first of which is
            //   interpreted as an item type of zero to denote the end of the list,
            //   and the remainder as needed to pad until the next 32-bit boundary. A
            //   chunk with zero items (four null octets) is valid but useless.
            int remainLength = data.length - index;
            if (remainLength > 0) {
                sdesItemList = new ArrayList<>();

                while (true) {
                    remainLength = data.length - index;
                    if (remainLength < 1) { break; }
                    if (remainLength == 1) {
                        SdesItem sdesItem = new SdesItem(SdesType.END, 0, null);
                        sdesItemList.add(sdesItem);
                        break;
                    } else {
                        byte[] curSdesItemHeader = new byte[SdesItem.MIN_LENGTH];
                        System.arraycopy(data, index, curSdesItemHeader, 0, SdesItem.MIN_LENGTH);
                        index += SdesItem.MIN_LENGTH;

                        int curSdesItemTextLength = curSdesItemHeader[1]; // (bytes)
                        if (curSdesItemTextLength > 0) {
                            remainLength = data.length - index;
                            if (remainLength <= curSdesItemTextLength) {
                                break;
                            }

                            byte[] curSdesItemText = new byte[curSdesItemTextLength];
                            System.arraycopy(data, index, curSdesItemText, 0, curSdesItemTextLength);
                            index += curSdesItemTextLength;

                            SdesItem sdesItem = new SdesItem(
                                    SdesItem.getTypeByIndex(curSdesItemHeader[0]),
                                    curSdesItemHeader[1],
                                    new String(curSdesItemText, StandardCharsets.UTF_8)
                            );
                            sdesItemList.add(sdesItem);
                        } else {
                            break;
                        }
                    }
                }
            }
        }
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    public byte[] getData() {
        byte[] data = new byte[MIN_LENGTH];
        int index = 0;

        // SSRC
        byte[] ssrcData = ByteUtil.intToBytes((int) ssrc, true);
        System.arraycopy(ssrcData, 0, data, index, ssrcData.length);
        index += ssrcData.length;

        // SdesItem List
        if (sdesItemList != null && !sdesItemList.isEmpty()) {
            int totalSdesItemSize = 0;
            for (SdesItem sdesItem : sdesItemList) {
                byte[] sdesItemData = sdesItem.getData();
                totalSdesItemSize += sdesItemData.length;
            }

            byte[] newData = new byte[data.length + totalSdesItemSize];
            System.arraycopy(data, 0, newData, 0, data.length);
            data = newData;

            for (SdesItem sdesItem : sdesItemList) {
                if (sdesItem == null) { continue; }

                byte[] sdesItemData = sdesItem.getData();
                System.arraycopy(sdesItemData, 0, data, index, sdesItemData.length);
                index += sdesItemData.length;
            }

            int bytes = data.length;
            int remainderBytesByMultiple = bytes % RtcpPacket.PACKET_MULTIPLE;
            if (remainderBytesByMultiple != 0) { // 4로 나누어 떨어지지 않으면 뒤에 남은 바이트 수만큼 패딩 바이트 추가
                int paddingLength = RtcpPacket.PACKET_MULTIPLE - remainderBytesByMultiple;
                newData = new byte[data.length + paddingLength];
                Arrays.fill(newData, (byte) 0);
                System.arraycopy(data, 0, newData, 0, data.length);
                data = newData;
            }
        }

        return data;
    }

    public void setData(long ssrc, List<SdesItem> sdesItemList) {
        this.ssrc = ssrc;
        this.sdesItemList = sdesItemList;
    }

    public long getSsrc() {
        return ssrc;
    }

    public void setSsrc(long ssrc) {
        this.ssrc = ssrc;
    }

    public List<SdesItem> getSdesItemList() {
        return sdesItemList;
    }

    public SdesItem getSdesItemByIndex(int index) {
        if (sdesItemList == null || index < 0 || index >= sdesItemList.size()) { return null; }
        return sdesItemList.get(index);
    }

    public void setSdesItemList(List<SdesItem> sdesItemList) {
        this.sdesItemList = sdesItemList;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
