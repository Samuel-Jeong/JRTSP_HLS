package com.rtsp.media;

import com.rtsp.media.codec.BasicCodec;
import com.rtsp.media.control.Buffer;
import com.rtsp.media.control.Control;
import com.rtsp.media.control.FrameProcessingControl;
import com.rtsp.media.exception.ResourceUnavailableException;
import com.rtsp.media.format.Format;
import com.rtsp.media.format.JpegFormat;
import com.rtsp.media.format.VideoFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;

import static com.rtsp.media.control.Plugin.*;


/**
 * @class public class Packetizer extends BasicCodec
 * @brief Packetizer class
 */
public class Packetizer extends BasicCodec {

    private static final Logger logger = LoggerFactory.getLogger(Packetizer.class);
    
    static final JpegFormat fJPEG = new JpegFormat();

    // I/O formats
    private VideoFormat inputFormat = null;
    private VideoFormat outputFormat = null;

    // default packet size of JPEG payload for RTP. can be changed
    // using encoding controls ?
    private static final int PACKET_SIZE = 1400;

    // current sequence number on RTP format packets.
    private int currentSeq = 0;

    // current timestamp on RTP format packets.
    //private long timestamp = (long) (System.currentTimeMillis() * Math.random());
    // next 4 varaibles used for RTP packetization
    private final Control[] controls;

    private boolean newFrame = true;
    private boolean dropFrame = false;
    private boolean minimal = false;

    private int offset = 0;
    private int frameLength = 0;
    private static final int J_SOF = 0xc0;
    private static final int J_SOF1 = 0xc1;

    private int decimation = -1;

    // default frame rate for RTP JPEG format. Should normally be
    // specified when frame rate is set.
    private static final int DEFAULT_FRAMERATE = 15;
    private float frameDuration = -1;

    /****************************************************************
     * Codec Methods
     ****************************************************************/

    // Initialize default formats.
    public Packetizer() {
        inputFormats = new VideoFormat[] {new VideoFormat(VideoFormat.JPEG)};
        outputFormats = new VideoFormat[] {new VideoFormat(VideoFormat.JPEG_RTP)};

        FrameProcessingControl fpc = new FrameProcessingControl() {
            public boolean setMinimalProcessing(boolean newMinimal) {
                minimal = newMinimal;
                return minimal;
            }

            public void setFramesBehind(float frames) {
                dropFrame = frames >= 1;
            }

            public Component getControlComponent() {
                return null;
            }

            public int getFramesDropped() {
                return 0;       ///XXX not implemented
            }

        };

        controls = new Control[1];
        controls[0] = fpc;
    }

    protected Format getInputFormat() {
        return inputFormat;
    }

    protected Format getOutputFormat() {
        return outputFormat;
    }

    // Return supported output formats
    public Format [] getSupportedOutputFormats(Format in) {
        if (in == null)
            return outputFormats;

        // Make sure the input is JPEG video format
        if (!verifyInputFormat(in)) {
            return new Format[0];
        }

        Format[] out = new Format[1];
        // check the frame Rate and if it is dont care, its needs
        // to be set to the default frame rate.

        if ( ((VideoFormat)in).getFrameRate() == VideoFormat.NOT_SPECIFIED) {
            out[0] = new VideoFormat(VideoFormat.JPEG_RTP,
                    ((VideoFormat) in).getSize(),
                    ((VideoFormat) in).getMaxDataLength(),
                    Format.byteArray,
                    DEFAULT_FRAMERATE);
        } else {
            out[0] = new VideoFormat(VideoFormat.JPEG_RTP,
                    ((VideoFormat) in).getSize(),
                    ((VideoFormat) in).getMaxDataLength(),
                    Format.byteArray,
                    ((VideoFormat) in).getFrameRate());
        }

        return out;
    }

    private boolean verifyInputFormat(Format input) {
        // -- by hsy to improve robustness & performance
        //if ((input instanceof VideoFormat) &&
        //    (input.getEncoding().equals(VideoFormat.JPEG)))
        //    return true;
        //return false;

        return fJPEG.matches(input);
    }

    public Format setInputFormat(Format input) {
        if (!verifyInputFormat(input)) {
            return null;
        }

        inputFormat = (VideoFormat)input;

        float rate = inputFormat.getFrameRate();
        if (rate != Format.NOT_SPECIFIED) {
            // frame duration in msec
            frameDuration = 1000 / rate;
        }

        if (opened) {
            outputFormat = (VideoFormat) getSupportedOutputFormats(input)[0];
        }

        return input;
    }

    private boolean matches(Format destFormat, Format[] sourceFormats) {
        for (Format format : sourceFormats) {
            if (format.matches(destFormat)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Format setOutputFormat(Format output) {
        if (!matches(output, outputFormats)) {
            return null;
        }

        outputFormat = (VideoFormat) output;
        return output;
    }

    @Override
    public void open() throws ResourceUnavailableException {
        if (inputFormat == null || outputFormat == null) {
            throw new ResourceUnavailableException("Incorrect formats set on JPEG Packetizer");
        }

        // Validate the sizes.
        Dimension size = inputFormat.getSize();
        if (size != null){
            // the JPEG packetizer cannot handle non standard format sizes,
            // so it checks the size here and will return null if the size
            // is not a multiple of 8 pixels
            if ((size.width % 8 != 0) || (size.height % 8 != 0)){
                logger.error("Class: " + this);
                logger.error("  can only packetize in sizes of multiple of 8 pixels.");
                throw new ResourceUnavailableException("Incorrect formats set on JPEG Packetizer");
            }
        }

        super.open();
    }

    public synchronized int process(Buffer inBuffer, Buffer outBuffer) {
        if (isEOM(inBuffer)) {
            propagateEOM(outBuffer);
            return BUFFER_PROCESSED_OK;
        }

        if (inBuffer.isDiscard()) {
            updateOutput(outBuffer, outputFormat, 0, 0);
            outBuffer.setDiscard(true);
            return OUTPUT_BUFFER_NOT_FILLED;
        }

        if (inBuffer.getLength() <= 0) {
            outBuffer.setDiscard(true);
            return OUTPUT_BUFFER_NOT_FILLED;
        }

        byte [] inData = (byte[]) inBuffer.getData();

        outBuffer.setFormat(outputFormat);

        Dimension size = inputFormat.getSize();
        int keyFrame = 0;

        if (newFrame) {
            if (dropFrame || minimal) {
                outBuffer.setDiscard(true);
                return BUFFER_PROCESSED_OK;
            }

            int tempdec = peekJPEGDecimation(inData, inBuffer.getLength());
            if (tempdec >= 0) {
                decimation = tempdec;
            }

            if (inputFormat instanceof JpegFormat) {
                stripTables(inBuffer);
            }

            frameLength = inBuffer.getLength();
            offset = 0;
            newFrame = false;
            keyFrame = Buffer.FLAG_KEY_FRAME;
        }

        //copy correct part of the encoded buffer into outputbuffer
        int copyLength;
        copyLength = Math.min(frameLength - offset, PACKET_SIZE);

        byte[] outData = (byte[])outBuffer.getData();
        if ((outData == null) || outData.length < copyLength + 8){
            outData = new byte[copyLength + 8];
            outBuffer.setData(outData);
        }

        int curOffset = offset + inBuffer.getOffset();
        int curCopyLength = copyLength - curOffset;
        if (curCopyLength < 0) {
            curCopyLength = 0;
        }

        System.arraycopy(
                inData, curOffset,
                outData, 8,
                curCopyLength
        );

        int qfactor = (inputFormat instanceof JpegFormat ?
                ((JpegFormat)inputFormat).getQFactor() : 80);
        decimation =  (inputFormat instanceof JpegFormat ?
                ((JpegFormat)inputFormat).getDecimation() : decimation);
        if (decimation == -1) {
            decimation = 1;
        }

        outBuffer.setLength(copyLength + 8);
        outBuffer.setOffset(0);
        //outBuffer.setTimeStamp(timestamp);
        outBuffer.setTimeStamp((long) (currentSeq * frameDuration));
        outBuffer.setSequenceNumber(currentSeq++);
        outBuffer.setFormat(outputFormat);
        outData[0] = 0;
        outData[1] = (byte) (offset >> 16);
        outData[2] = (byte) (offset >> 8);
        outData[3] = (byte) (offset);
        outData[4] = (byte) decimation;
        outData[5] = (byte) qfactor;
        outData[6] = (byte) (size.width/8);
        outData[7] = (byte) (size.height/8);

        offset += copyLength;

        outBuffer.setFlags(outBuffer.getFlags() | keyFrame);

        if (offset == frameLength) {
            outBuffer.setFlags(outBuffer.getFlags() | Buffer.FLAG_RTP_MARKER);
            newFrame = true;
            return BUFFER_PROCESSED_OK;
        }

        return INPUT_BUFFER_NOT_CONSUMED;

    }// end of process()

    int peekJPEGDecimation(byte [] data, int dataLen) {
        int i = 0;
        int code;
        if ((data[0] & 0xFF) != 0xFF || data[1] == 0)
            return -1;
        while (i < dataLen - 2) {
            if ((data[i] & 0xFF) == 0xFF) {
                i++;
                code = data[i] & 0xFF;
                i++;
                if (code == J_SOF || code == J_SOF1) {
                    return getDecimationFromSOF(data, i, dataLen);
                }
            } else
                i++;
        }
        return -1;
    }

    private void stripTables(Buffer inb) {
        byte [] data = (byte[]) inb.getData();
        int offset = inb.getOffset();
        int length = inb.getLength();
        int i = offset;
        while (i < length + offset - 8) {
            if (data[i] == (byte)0xFF) {
                if (data[i+1] == (byte)0xDA) {
                    // Found SOS Start of Scan marker
                    // Skip over this block
                    int blockSize = ((data[i+2] & 0xFF) << 8) |
                            (data[i+3] & 0xFF);
                    i += 2 + blockSize;
                    System.arraycopy(data, i,
                            data, 0,
                            length + offset - i);
                    inb.setOffset(0);
                    inb.setLength(length + offset - i);
                    break;
                }
            }
            i++;
        }
    }

    int getDecimationFromSOF(byte [] data, int i, int length) {
        int sectionLen;
        int dummy;
        int ncomp;
        int deccode;
        int hsf, vsf;
        int id;

        sectionLen = (data[i++] & 0xFF) << 8;
        sectionLen |= (data[i++] & 0xFF);

        i += 5; // skip precision, 2height, 2width
        ncomp = (data[i++] & 0xFF);

        if (sectionLen != ncomp * 3 + 8)
            System.err.println("Bogus SOF length");

        id = data[i++] & 0xFF;
        deccode = data[i++] & 0xFF;
        hsf = (deccode >> 4) & 15;
        vsf = (deccode     ) & 15;
        if (vsf == 2 && hsf == 2)
            return JpegFormat.DEC_420;
        else if (vsf == 1 && hsf == 1)
            return JpegFormat.DEC_444;
        else
            return JpegFormat.DEC_422;
    }
}