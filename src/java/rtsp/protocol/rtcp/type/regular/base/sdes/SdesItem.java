package rtsp.protocol.rtcp.type.regular.base.sdes;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rtsp.protocol.base.ByteUtil;

import java.nio.charset.StandardCharsets;

public class SdesItem {

    private static final Logger logger = LoggerFactory.getLogger(SdesItem.class);

    /**
     * Only the CNAME item is mandatory.
     * Some items shown here may be useful only for particular profiles,
     * but the item types are all assigned from one common space
     * to promote shared use and to simplify profile-independent applications.
     * Additional items may be defined in a profile by registering the type numbers with IANA.
     */

    ////////////////////////////////////////////////////////////
    // VARIABLES
    public static final int MIN_LENGTH = 2; // bytes
    public static final int LIMIT_TEXT_LENGTH = 255; // bytes

    // TYPE
    private SdesType sdesType = SdesType.UNKNOWN; // (8 bits)

    // LENGTH
    private int length  = 0; // (8 bits)

    // TEXT
    // The text is encoded according to the UTF-2 encoding specified in
    //   Annex F of ISO standard 10646 [12,13]. This encoding is also known as
    //   UTF-8 or UTF-FSS. It is described in "File System Safe UCS
    //   Transformation Format (FSS_UTF)", X/Open Preliminary Specification,
    //   Document Number P316 and Unicode Technical Report #4. US-ASCII is a
    //   subset of this encoding and requires no additional encoding.
    private String text = null; // (until 255 bytes (2048 bits))
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // CONSTRUCTOR
    public SdesItem(SdesType sdesType, int length, String text) {
        this.sdesType = sdesType;
        this.length = (short) length;
        this.text = text;
    }

    public SdesItem() {}

    public SdesItem(byte[] data) {
        if (data.length >= MIN_LENGTH) {
            int index = 0;

            // TYPE
            byte[] typeData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, typeData, ByteUtil.NUM_BYTES_IN_INT - 1, 1);
            int sdesTypeIndex = ByteUtil.bytesToInt(typeData, true);
            sdesType = getTypeByIndex(sdesTypeIndex);
            index += 1;

            // LENGTH
            byte[] lengthData = new byte[ByteUtil.NUM_BYTES_IN_INT];
            System.arraycopy(data, index, lengthData, ByteUtil.NUM_BYTES_IN_INT - 1, 1);
            length = ByteUtil.bytesToInt(lengthData, true);
            index += 1;

            // TEXT
            int textLength = data.length - MIN_LENGTH;
            if (textLength > 0) {
                if (textLength > LIMIT_TEXT_LENGTH) {
                    textLength = LIMIT_TEXT_LENGTH;
                }

                byte[] textData = new byte[textLength];
                System.arraycopy(data, index, textData, 0, textLength);
                text = new String(textData, StandardCharsets.UTF_8);
            }
        }
    }
    ////////////////////////////////////////////////////////////

    ////////////////////////////////////////////////////////////
    // FUNCTIONS
    public static SdesType getTypeByIndex(int index) {
        switch (index) {
            case 0:
                return SdesType.END;
            case 1:
                return SdesType.CNAME;
            case 2:
                return SdesType.NAME;
            case 3:
                return SdesType.EMAIL;
            case 4:
                return SdesType.PHONE;
            case 5:
                return SdesType.LOC;
            case 6:
                return SdesType.TOOL;
            case 7:
                return SdesType.PRIV;
            default:
                return SdesType.UNKNOWN;
        }
    }

    public byte[] getData() {
        byte[] data;
        if (sdesType == SdesType.END) {
            data = new byte[]{ 0 };
        } else {
            data = new byte[MIN_LENGTH];
            int index = 0;

            byte[] typeData = { (byte) sdesType.ordinal() };
            System.arraycopy(typeData, 0, data, index, 1);
            index += 1;

            byte[] lengthData = { (byte) length };
            System.arraycopy(lengthData, 0, data, index, 1);
            index += 1;

            if (text != null) {
                byte[] textData = text.getBytes(StandardCharsets.UTF_8);
                int textLength = textData.length;

                if (textLength > 0) {
                    byte[] newData = new byte[data.length + textLength];
                    System.arraycopy(data, 0, newData, 0, data.length);
                    data = newData;

                    System.arraycopy(textData, 0, data, index, textLength);
                }
            }
        }

        return data;
    }

    public void setData(SdesType sdesType, int length, String text) {
        this.sdesType = sdesType;
        this.length = length;
        this.text = text;
    }

    public SdesType getSdesType() {
        return sdesType;
    }

    public void setSdesType(SdesType sdesType) {
        this.sdesType = sdesType;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }
    ////////////////////////////////////////////////////////////

}
