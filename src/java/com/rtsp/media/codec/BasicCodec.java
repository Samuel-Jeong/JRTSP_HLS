package com.rtsp.media.codec;

import com.rtsp.media.control.Buffer;
import com.rtsp.media.exception.ResourceUnavailableException;
import com.rtsp.media.format.Format;
import com.rtsp.media.format.RgbFormat;
import com.rtsp.media.format.VideoFormat;

import java.awt.*;

import static com.rtsp.media.control.Plugin.BUFFER_PROCESSED_OK;
import static com.rtsp.media.control.Plugin.INPUT_BUFFER_NOT_CONSUMED;

/**
 * @class public abstract class BasicCodec implements Codec
 * @brief BasicCodec abstract class
 */
public abstract class BasicCodec implements Codec {
    private static final boolean DEBUG = true;

    protected Format inputFormat;

    protected Format outputFormat;

    protected boolean opened = false;

    protected Format[] inputFormats=new Format[0];

    protected Format[] outputFormats=new Format[0];

    protected boolean pendingEOM = false;

    public Format setInputFormat(Format input) {
        inputFormat = input;
        return input;
    }

    public Format setOutputFormat(Format output) {
        outputFormat = output;
        return output;
    }

    protected Format getInputFormat() {
        return inputFormat;
    }

    protected Format getOutputFormat() {
        return outputFormat;
    }

    public void reset() {
    }

    public void open() throws ResourceUnavailableException {
        opened = true;
    }

    public void close() {
        opened = false;
    }


    public Format [] getSupportedInputFormats() {
        return inputFormats;
    }

    protected RgbFormat updateRGBFormat(VideoFormat newFormat, RgbFormat outputFormat) {
        Dimension size = newFormat.getSize();
        RgbFormat oldFormat = outputFormat;
        int lineStride = size.width * oldFormat.getPixelStride();
        RgbFormat newRGB = new RgbFormat(size,
                lineStride * size.height,
                oldFormat.getDataType(),
                newFormat.getFrameRate(),
                oldFormat.getBitsPerPixel(),
                oldFormat.getRedMask(),
                oldFormat.getGreenMask(),
                oldFormat.getBlueMask(),
                oldFormat.getPixelStride(),
                lineStride,
                oldFormat.getFlipped(),
                oldFormat.getEndian()
        );
        return newRGB;
    }

    protected boolean isEOM(Buffer inputBuffer) {
        return inputBuffer.isEOM();
    }

    protected void propagateEOM(Buffer outputBuffer) {
        updateOutput(outputBuffer, getOutputFormat(), 0, 0);
        outputBuffer.setEOM(true);
    }

    protected void updateOutput(Buffer outputBuffer, Format format,int length, int offset) {
        outputBuffer.setFormat(format);
        outputBuffer.setLength(length);
        outputBuffer.setOffset(offset);
    }

    protected boolean checkInputBuffer(Buffer inputBuffer){
        boolean fError= !isEOM(inputBuffer) &&
                (inputBuffer.getFormat() == null || !checkFormat(inputBuffer.getFormat()));

        if (DEBUG)
            if (fError)
                System.out.println(getClass().getName()+" : [error] checkInputBuffer");

        return !fError;

    }

    protected boolean checkFormat(Format format) {
        return true;
    }

    protected int checkEOM(Buffer inputBuffer,Buffer outputBuffer) {

        processAtEOM(inputBuffer,outputBuffer); // process tail of input

        // this is a little tricky since we have to output two frames now:
        // one to close former session, another to signle EOM
        if (outputBuffer.getLength() > 0 ) {
            pendingEOM = true;
            return BUFFER_PROCESSED_OK | INPUT_BUFFER_NOT_CONSUMED;
        } else {
            // in case we have nothing in the output, we are done
            propagateEOM(outputBuffer);
            return BUFFER_PROCESSED_OK;
        }
    }

    protected int processAtEOM(Buffer inputBuffer,Buffer outputBuffer) {
        return 0;
    }

    protected int getArrayElementSize(Class type) {
        if (type == Format.intArray)
            return 4;
        else if (type == Format.shortArray)
            return 2;
        else if (type == Format.byteArray)
            return 1;
        else
            return 0;
    }
}









