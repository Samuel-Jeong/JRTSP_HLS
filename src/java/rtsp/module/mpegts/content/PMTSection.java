package rtsp.module.mpegts.content;

import org.jcodec.common.io.NIOUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;


/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * <p>
 * Represents PMT ( Program Map Table ) of the MPEG Transport stream
 * <p>
 * This section contains information about streams of an individual program, a
 * program usually contains two or more streams, such as video, audio, text,
 * etc..
 *
 * @author The JCodec project
 */
public class PMTSection extends PSISection {

    private final int pcrPid;

    public PMTSection(PSISection psi, int pcrPid) {
        super(psi);

        this.pcrPid = pcrPid;
    }

    public static PMTSection parse(ByteBuffer data) {
        PSISection psi = PSISection.parse(data);

        int w1 = data.getShort() & 0xffff;
        int pcrPid = w1 & 0x1fff;

        int w2 = data.getShort() & 0xffff;
        int programInfoLength = w2 & 0xfff;

        return new PMTSection(psi, pcrPid);
    }

    static List<Tag> parseTags(ByteBuffer bb) {
        List<Tag> tags = new ArrayList<Tag>();
        while (bb.hasRemaining()) {
            int tag = bb.get();
            int tagLen = bb.get();
            tags.add(new Tag(tag, NIOUtils.read(bb, tagLen)));
        }
        return tags;
    }

    public int getPcrPid() {
        return pcrPid;
    }

    public static class Tag {
        private final int tag;
        private final ByteBuffer content;

        public Tag(int tag, ByteBuffer content) {
            this.tag = tag;
            this.content = content;
        }

        public int getTag() {
            return tag;
        }

        public ByteBuffer getContent() {
            return content;
        }
    }

}