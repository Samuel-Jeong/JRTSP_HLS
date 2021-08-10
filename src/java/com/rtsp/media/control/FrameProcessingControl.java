package com.rtsp.media.control;

/**
 * @interface public interface FrameProcessingControl extends Control
 * @brief FrameProcessingControl interface
 */
public interface FrameProcessingControl extends Control {

    /**
     * Sets the number of output frames the codec is lagging behind.
     * This is a hint to do minimal processing for the next
     * <code> numFrames </code> frames in order to catch up.
     * @param numFrames  the number of frames the codec is lagging behind
     */
    public void setFramesBehind(float numFrames);

    /**
     * Sets the minimal processing mode. Minimal processing is doing only
     * the needed calculations in order to keep the codec state, without
     * outputting anything.
     * Returns false if miminal processing is not set.
     * @param newMinimalProcessing new minimal processign mode.
     * @return the actual mode set.
     *
     **/
    public boolean setMinimalProcessing(boolean newMinimalProcessing);

    /**
     * Returns the number of output frames that were dropped during encoding
     * since the last call to this method.
     * @return the number of output frames that were dropped during encoding
     * since the last call to this method.
     */
    public int getFramesDropped();

}