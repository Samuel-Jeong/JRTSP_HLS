package rtsp.protocol.rtcp.type.regular;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.protocol.rtcp.base.RtcpFormat;
import rtsp.protocol.rtcp.type.regular.base.sdes.SdesChunk;

import java.util.ArrayList;
import java.util.List;

public class RtcpSourceDescription extends RtcpFormat {

    private static final Logger logger = LoggerFactory.getLogger(RtcpSourceDescription.class);

    /**
     *  0                   1                   2                   3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |                          SSRC/CSRC_1                          | chunk
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+   1
     * |                           SDES items                          |
     * |                              ...                              |
     * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
     * |                          SSRC/CSRC_2                          | chunk
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+   2
     * |                           SDES items                          |
     * |                              ...                              |
     * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
     *
     *  0               1               2               3
     *  0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     * |    CNAME=1    |     length    | user and domain name         ...
     * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
     */

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int CHUNK_LIMIT = 31; // bytes (2^5 - 1, 5 is the bits of resource count of header)

    // SDES CHUNK LIST
    private List<SdesChunk> sdesChunkList = null; // Limit 31 chunks
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public RtcpSourceDescription(List<SdesChunk> sdesChunkList) {
        this.sdesChunkList = sdesChunkList;
    }

    public RtcpSourceDescription() {}

    public RtcpSourceDescription(byte[] data) {
        // SDES CHUNK LIST
        // Each chunk consists of an SSRC/CSRC identifier followed by a list of
        //   zero or more items, which carry information about the SSRC/CSRC. Each
        //   chunk starts on a 32-bit boundary. Each item consists of an 8-bit
        //   type field, an 8-bit octet count describing the length of the text
        //   (thus, not including this two-octet header), and the text itself.
        //   Note that the text can be no longer than 255 octets (bytes), but this is
        //   consistent with the need to limit RTCP bandwidth consumption.
        // 1) Chunk 개수는 헤더에서 결정
        // 2) Chunk 는 SSRC 로 구분
        // 3) Chunk 는 마지막 바이트가 0 인 것으로 구분
        int dataLength = data.length;
        if (dataLength > 0) {
            int index = 0;
            sdesChunkList = new ArrayList<>();
            List<Integer> chunkEndPositionList = new ArrayList<>();
            List<Integer> chunkStartPositionList = new ArrayList<>();
            chunkStartPositionList.add(0);

            for (int i = index; i < dataLength; i++) {
                if (data[i] == 0) {
                    chunkEndPositionList.add(i);

                    if ((i + 1) < dataLength) { // 다음 청크가 없으면 start position 을 체크하지 않는다.
                        for (int j = i + 1; j < dataLength; j++) {
                            if (data[j] != 0) {
                                chunkStartPositionList.add(j);
                                break;
                            }
                            i++;
                        }
                    }
                }
            }

            int chunkCount = chunkEndPositionList.size();
            if (chunkCount > CHUNK_LIMIT) {
                chunkCount = CHUNK_LIMIT;
            }

            int curChunkDataLength;
            for (int i = 0; i < chunkCount; i++) {
                curChunkDataLength = chunkEndPositionList.get(i) - chunkStartPositionList.get(i) + 1;
                if (curChunkDataLength > 0) {
                    byte[] curChunkData = new byte[curChunkDataLength];
                    System.arraycopy(data, chunkStartPositionList.get(i), curChunkData, 0, curChunkDataLength);
                    index += curChunkDataLength;
                    if (curChunkData[0] == 0) { break; }

                    SdesChunk sdesChunk = new SdesChunk(curChunkData);
                    sdesChunkList.add(sdesChunk);
                }
            }
        }
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    @Override
    public byte[] getData() {
        int totalSdesChunkSize = getTotalSdesChunkSize();
        if (totalSdesChunkSize > 0) {
            byte[] data = new byte[totalSdesChunkSize];
            int index = 0;

            // SdesChunk List
            for (SdesChunk sdesChunk : sdesChunkList) {
                if (sdesChunk == null) { continue; }

                byte[] sdesChunkData = sdesChunk.getData();
                System.arraycopy(sdesChunkData, 0, data, index, sdesChunkData.length);
                index += sdesChunkData.length;
            }

            return data;
        }

        return null;
    }

    public void setData(List<SdesChunk> sdesChunkList) {
        this.sdesChunkList = sdesChunkList;
    }

    public int getTotalSdesChunkSize() {
        int totalSize = 0;

        for (SdesChunk sdesChunk : sdesChunkList) {
            if (sdesChunk == null) { continue; }

            byte[] sdesChunkData = sdesChunk.getData();
            totalSize += sdesChunkData.length;
        }

        return totalSize;
    }

    public List<SdesChunk> getSdesChunkList() {
        return sdesChunkList;
    }

    public SdesChunk getSdesChunkByIndex(int index) {
        if (sdesChunkList == null || index < 0 || index >= sdesChunkList.size()) { return null; }
        return sdesChunkList.get(index);
    }

    public void setSdesChunkList(List<SdesChunk> sdesChunkList) {
        this.sdesChunkList = sdesChunkList;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
