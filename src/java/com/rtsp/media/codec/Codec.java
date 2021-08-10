package com.rtsp.media.codec;

import com.rtsp.media.control.Buffer;
import com.rtsp.media.format.Format;

/**
 * @interface public interface Codec
 * @brief Codec interface
 */
public interface Codec {

    /**
     * Lists all of the input formats that this codec accepts.
     * @return An array that contains the supported input <code>Formats</code>.
     */
    public Format[] getSupportedInputFormats();

    /**
     * Lists the output formats that this codec can generate.
     * If <code>input</code> is non-null, this method lists the possible
     * output formats that can be generated from input data of the specified <code>Format</code>.
     * If <code>input</code> is null, this method lists
     * all of the output formats supported by this plug-in.
     * @param input The <code>Format</code> of the data to be used as input to the plug-in.
     * @return An array that contains the supported output <code>Formats</code>.
     */
    public Format [] getSupportedOutputFormats(Format input);

    /**
     * Sets the format of the data to be input to this codec.
     * @param format The <code>Format</code> to be set.
     * @return The <code>Format</code> that was set, which might be the
     * supported <code>Format</code> that most closely matches the one specified.
     * Returns null if the specified <code>Format</code> is not supported and
     * no reasonable match could be found.
     */
    public Format setInputFormat(Format format);

    /**
     * Sets the format for the data this codec outputs.
     * @param format The <code>Format</code> to be set.
     * @return The <code>Format</code> that was set, which might be the
     * <code>Format</code> that most closely matched the one specified.
     * Returns null if the specified <code>Format</code> is not supported and
     * no reasonable match could be found.
     */
    public Format setOutputFormat(Format format);

    /**
     * Performs the media processing defined by this codec.
     * @param input The <code>Buffer</code> that contains the media data to be processed.
     * @param output The <code>Buffer</code> in which to store the processed media data.
     * @return <CODE>BUFFER_PROCESSED_OK</CODE> if the processing is successful.  Other
     * possible return codes are defined in <CODE>PlugIn</CODE>.
     */
    public int process(Buffer input, Buffer output);

}
