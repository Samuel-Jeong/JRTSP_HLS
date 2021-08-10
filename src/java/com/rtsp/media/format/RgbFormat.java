package com.rtsp.media.format;

import java.awt.*;

/**
 * @class public class RgbFormat extends VideoFormat
 * @brief RgbFormat class
 */
public class RgbFormat extends VideoFormat {

    /** Mask value for the Red component. */
    protected int redMask = Format.NOT_SPECIFIED;

    /** Mask value for the Green component. */
    protected int greenMask = Format.NOT_SPECIFIED;

    /** Mask value for the Blue component. */
    protected int blueMask = Format.NOT_SPECIFIED;

    /**
     * The number of bits required to represent a pixel, including all three
     * color components.
     */
    protected int bitsPerPixel = Format.NOT_SPECIFIED;

    /** Increment value of the array index from one pixel to the next. */
    protected int pixelStride = Format.NOT_SPECIFIED;

    /**
     * Increment value of the array index from the first pixel on line n
     * to the first pixel on line n+1.
     */
    protected int lineStride = Format.NOT_SPECIFIED;

    /** Indicates whether or not the lines in the video frame are flipped vertically
     * (upside down).
     */
    protected int flipped = Format.NOT_SPECIFIED;

    /** Endian ordering of the data where applicable */
    protected int endian = Format.NOT_SPECIFIED;


    public static final int BIG_ENDIAN = 0;
    public static final int LITTLE_ENDIAN = 1;

    private static final String ENCODING = VideoFormat.RGB;

    /**
     * Constructs a default <CODE>RGBFormat</CODE>.
     */
    public RgbFormat() {
        super(ENCODING);
        dataType = null;
    }

    /**
     * Constructs an <CODE>RGBFormat</CODE> object with the specified properties.
     * The pixel stride is
     * initialized to the default for the specified data type and bits per pixel.
     * The line stride is initialized using the default pixel stride and specified frame
     * width.
     * The image is not flipped and the endian is LITTLE_ENDIAN.
     * @param size  A <CODE>Dimension</CODE> that specifies the frame size.
     * @param maxDataLength  The maximum length of a data chunk.
     * @param dataType  The type of the data. For example, byte array.
     * @param frameRate  The frame rate.
     * @param bitsPerPixel The number of bits representing a pixel.
     * @param red  The mask for the red color component.
     * @param green  The mask for the green color component.
     * @param blue  The mask for the blue color component.
     */
    public RgbFormat(Dimension size, int maxDataLength, Class dataType,
                     float frameRate,
                     int bitsPerPixel,
                     int red, int green, int blue) {
        super(ENCODING, size, maxDataLength, dataType, frameRate);
        this.bitsPerPixel = bitsPerPixel;
        this.redMask = red;
        this.greenMask = green;
        this.blueMask = blue;
        if (bitsPerPixel != Format.NOT_SPECIFIED && dataType != null) {
            pixelStride = bitsPerPixel / 8;
            if (dataType != Format.byteArray)
                pixelStride = 1;
        } else {
            pixelStride = Format.NOT_SPECIFIED;
        }
        if (size != null && pixelStride != Format.NOT_SPECIFIED)
            lineStride = pixelStride * size.width;
        else
            lineStride = Format.NOT_SPECIFIED;
        flipped = Format.FALSE;
        if (bitsPerPixel == 16 && dataType == Format.byteArray)
            endian = LITTLE_ENDIAN;
        else
            endian = Format.NOT_SPECIFIED;
    }

    /**
     * Constructs an <CODE>RGBFormat</CODE> object with the specified properties.
     * @param size  A <CODE>Dimension</CODE> that specifies the frame size.
     * @param maxDataLength  The maximum length of a data chunk.
     * @param dataType  The type of the data. For example, byte array.
     * @param frameRate  The frame rate.
     * @param bitsPerPixel The number of bits representing a pixel.
     * @param red  The mask for the red color component.
     * @param green  The mask for the green color component.
     * @param blue  The mask for the blue color component.
     * @param pixelStride  The number of array elements between adjacent pixels.
     * @param flipped  Indicates whether or not the  lines in the video frame are
     * flipped vertically (upside down). <CODE>Format.TRUE</CODE> indicates the image is flipped,
     * <CODE>Format.FALSE</CODE> indicates that it is not.
     * @param endian The byte ordering used for this
     * <code>RGBFormat</code>--<CODE>BIG_ENDIAN</CODE> or <CODE>LITTLE_ENDIAN</CODE>.
     */
    public RgbFormat(Dimension size, int maxDataLength, Class dataType,
                     float frameRate,
                     int bitsPerPixel,
                     int red, int green, int blue,
                     int pixelStride, int lineStride, int flipped, int endian) {
        super(ENCODING, size, maxDataLength, dataType, frameRate);
        this.bitsPerPixel = bitsPerPixel;
        this.redMask = red;
        this.greenMask = green;
        this.blueMask = blue;
        this.pixelStride = pixelStride;
        this.lineStride = lineStride;
        this.flipped = flipped;
        this.endian = endian;
    }

    /**
     * Gets the number of bits required per pixel of data.
     * @return An integer representing the number of bits per pixel.
     */
    public int getBitsPerPixel() {
        return bitsPerPixel;
    }

    /**
     * Gets the mask for the red component.
     * @return The red mask.
     */
    public int getRedMask() {
        return redMask;
    }

    /**
     * Gets the mask for the green component.
     * @return The green mask.
     */
    public int getGreenMask() {
        return greenMask;
    }

    /**
     * Gets the mask for the blue component.
     * @return The blue mask.
     */
    public int getBlueMask() {
        return blueMask;
    }

    /**
     * Gets the pixel stride--the number of array elements between adjacent pixels.
     * @return An integer representing the  pixel stride.
     */
    public int getPixelStride() {
        return pixelStride;
    }

    /**
     * Gets the line stride--the number of array elements between adjacent rows of pixels.
     * @return An integer representing the  line stride.
     */
    public int getLineStride() {
        return lineStride;
    }

    /**
     * Checks whether or not the video image is vertically flipped.
     *
     * @return <CODE>Format.TRUE</CODE> if the video is flipped, <CODE>Format.FALSE</CODE>
     * if it is not.
     */
    public int getFlipped() {
        return flipped;
    }

    /**
     * Gets the endian ordering of the data for unpacked 16-bit data.
     * @return An integer representing the endian ordering: BIG_ENDIAN, LITTLE_ENDIAN or NOT_SPECIFIED
     */
    public int getEndian() {
        return endian;
    }

    /**
     * Creates a clone of this <CODE>RGBFormat</CODE>.
     * @return A clone of this <CODE>RGBFormat</CODE>.
     */
    public Object clone() {
        RgbFormat f = new RgbFormat(size,
                maxDataLength,
                dataType,
                frameRate,
                bitsPerPixel,
                redMask,
                greenMask,
                blueMask,
                pixelStride,
                lineStride,
                flipped,
                endian);
        f.copy(this);
        return f;
    }

    /**
     * Copies  the attributes from the specified <CODE>Format</CODE> into
     * this <CODE>RGBFormat</CODE>.
     * @param f The <CODE>Format</CODE> to copy the attributes from.
     */
    protected void copy(Format f) {
        super.copy(f);
        if (f instanceof RgbFormat) {
            RgbFormat other = (RgbFormat) f;
            bitsPerPixel = other.bitsPerPixel;
            redMask = other.redMask;
            greenMask = other.greenMask;
            blueMask = other.blueMask;
            pixelStride = other.pixelStride;
            lineStride = other.lineStride;
            flipped = other.flipped;
            endian = other.endian;
        }
    }

    /**
     * Compares the specified <CODE>Format</CODE> with this <code>RGBFormat</code>.
     * Returns <CODE>true</CODE> only if the specified <CODE>Format</CODE>
     * is a <CODE>RGBFormat</CODE> object and all of
     * its attributes are identical to
     * the attributes in this <code>RGBFormat</code>.
     * @param format  The <CODE>Format</CODE> to compare.
     * @return true if the specified <CODE>Format</CODE> is the same as this one.
     */
    public boolean equals(Object format) {
        if (format instanceof RgbFormat) {
            RgbFormat other = (RgbFormat) format;

            return super.equals(format) &&
                    bitsPerPixel == other.bitsPerPixel &&
                    redMask == other.redMask &&
                    greenMask == other.greenMask &&
                    blueMask == other.blueMask &&
                    pixelStride == other.pixelStride &&
                    lineStride == other.lineStride &&
                    endian == other.endian &&
                    flipped == other.flipped;
        } else
            return false;
    }

    /**
     * Checks whether or not the specified <CODE>Format</CODE> <EM>matches</EM>
     * this <CODE>RGBFormat</CODE>.
     * Matches only compares the attributes that are defined in the specified
     * <CODE>Format</CODE>,
     * unspecified attributes are ignored.
     * <p>
     * The two <CODE>Format</CODE> objects do not have to be of the same class
     * to match.  For example, if "A" are "B" are being compared, a
     * match is possible if "A" is derived from "B"
     * or "B" is derived from "A". (The compared attributes must still match,
     * or <CODE>matches</CODE> fails.)
     * @param format The <CODE>Format</CODE> to compare with this one.
     * @return <CODE>true</CODE> if the specified <CODE>Format</CODE>
     * matches this one, <CODE>false</CODE> if it does not.
     */
    public boolean matches(Format format) {
        if (!super.matches(format))
            return false;
        if (!(format instanceof RgbFormat))
            return true;

        RgbFormat other = (RgbFormat) format;

        boolean returnVal =
                (bitsPerPixel == Format.NOT_SPECIFIED || other.bitsPerPixel == Format.NOT_SPECIFIED ||
                        bitsPerPixel == other.bitsPerPixel) &&

                        (redMask == Format.NOT_SPECIFIED || other.redMask == Format.NOT_SPECIFIED ||
                                redMask == other.redMask) &&
                        (greenMask == Format.NOT_SPECIFIED || other.greenMask == Format.NOT_SPECIFIED ||
                                greenMask == other.greenMask) &&
                        (blueMask == Format.NOT_SPECIFIED || other.blueMask == Format.NOT_SPECIFIED ||
                                blueMask == other.blueMask) &&

                        (pixelStride == Format.NOT_SPECIFIED || other.pixelStride == Format.NOT_SPECIFIED ||
                                pixelStride == other.pixelStride) &&

                        (endian == Format.NOT_SPECIFIED || other.endian == Format.NOT_SPECIFIED ||
                                endian == other.endian) &&

                        (flipped == Format.NOT_SPECIFIED || other.flipped == Format.NOT_SPECIFIED ||
                                flipped == other.flipped);

        return returnVal;
    }

    /**
     * Finds the attributes shared by two matching <CODE>Format</CODE> objects.
     * If the specified <CODE>Format</CODE> does not match this one, the result
     * is undefined.
     * @return A <CODE>Format</CODE> object
     * with its attributes set to those attributes common to both
     * <CODE>Format</CODE> objects.
     * @see #matches
     */
    public Format intersects(Format format) {
        Format fmt;
        if ((fmt = super.intersects(format)) == null)
            return null;
        if (!(format instanceof RgbFormat))
            return fmt;
        RgbFormat other = (RgbFormat)format;
        RgbFormat res = (RgbFormat)fmt;
        res.bitsPerPixel = (bitsPerPixel != Format.NOT_SPECIFIED ?
                bitsPerPixel : other.bitsPerPixel);
        res.pixelStride = (pixelStride != Format.NOT_SPECIFIED ?
                pixelStride : other.pixelStride);
        res.lineStride = (lineStride != Format.NOT_SPECIFIED ?
                lineStride : other.lineStride);
        res.redMask = (redMask != Format.NOT_SPECIFIED ?
                redMask : other.redMask);
        res.greenMask = (greenMask != Format.NOT_SPECIFIED ?
                greenMask : other.greenMask);
        res.blueMask = (blueMask != Format.NOT_SPECIFIED ?
                blueMask : other.blueMask);
        res.flipped = (flipped != Format.NOT_SPECIFIED ?
                flipped : other.flipped);
        res.endian = (endian != Format.NOT_SPECIFIED ?
                endian : other.endian);

        return res;
    }

    /**
     * Generate a format that's less restrictive than this format but
     * contains the basic attributes that will make this resulting format
     * useful for format matching.
     * @return A <CODE>Format</CODE> that's less restrictive than the
     * this format.
     */
    public Format relax() {
        RgbFormat fmt;
        if ((fmt = (RgbFormat)super.relax()) == null)
            return null;

        fmt.lineStride = Format.NOT_SPECIFIED;
        fmt.pixelStride = Format.NOT_SPECIFIED;

        return fmt;
    }

    /**
     * Gets a <CODE>String</CODE> representation of the attributes of this
     * <CODE>RGBFormat</CODE>.
     * For example: "RGB, 352x240, ...".
     * @return A <CODE>String</CODE> that describes the format attributes.
     */
    public String toString() {
        String s = getEncoding().toUpperCase();
        if (size != null)
            s += ", " + size.width + "x" + size.height;
        if (frameRate != Format.NOT_SPECIFIED)
            s += ", FrameRate=" + ((int)(frameRate * 10) / 10f);
        if (maxDataLength != Format.NOT_SPECIFIED)
            s += ", Length=" + maxDataLength;
        s += ", " + bitsPerPixel + "-bit";
        s += ", Masks=" + redMask + ":" + greenMask + ":" + blueMask;
        if (pixelStride != 1)
            s += ", PixelStride=" + pixelStride;
        s += ", LineStride=" + lineStride;
        if (flipped != Format.NOT_SPECIFIED)
            s += (flipped == Format.TRUE? ", Flipped" : "");
        if (dataType == Format.byteArray && bitsPerPixel == 16 && endian != Format.NOT_SPECIFIED)
            s += (endian == BIG_ENDIAN? ", BigEndian" : ", LittleEndian");
        if (dataType != null && dataType != Format.byteArray)
            s += ", " + dataType;
        return s;
    }
}
